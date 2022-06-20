package app.flows.inbound.compiler

import app.Type._
import app.flows.{ComponentId, resolved => In, typed => Out}
import app.{Error, Type}
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.traverse._

private[inbound] object semantic {

  def typecheck(graph: In.CreateFlowRequest): Either[Error.GraphValidationFailed, Out.CreateFlowRequest] = {
    // all starting points to traverse the graph
    val sinks = graph.components.collect {
      case (id, component) if component.isSink => id
    }.toList

    val checkResult = context.flatMap { ctx =>
      // we don't rewrite graphs in this phase
      require(
        (ctx.in.size - sinks.size) == ctx.out.size,
        "Not all components were connected to sinks."
      )
    }

    val result = (sinks.traverse(typeCheckSink(_)) <* checkResult)
      .run(Context.initial(graph.components))
      .map { case (_, sinks) =>
        Out.CreateFlowRequest(sinks)
      }

    result.left.map(Error.GraphValidationFailed(_))
  }

  private type Run[A] = Check[Context, String, A]

  final private case class Context(
    in: Map[ComponentId, In.Component],
    out: Map[ComponentId, Out.Stream],
    enclosing: Option[ComponentId]
  )

  private object Context {

    def initial(in: Map[ComponentId, In.Component]): Context =
      Context(in, Map.empty, None)
  }

  private val context: Run[Context] =
    Check.getState[Context]

  private def updateContext(f: Context => Context) =
    Check.updateState(f)

  private def fail(msg: String): Run[Nothing] =
    context.flatMap { ctx =>
      Check.fail(s"${ctx.enclosing.fold("")(id => s"[${id.value}]: ")}$msg")
    }

  private def pure[A](a: A): Run[A] =
    Check.done(a)

  private def pass: Run[Unit] =
    pure(())

  private def require(
    bool: Boolean,
    msg: String
  ): Run[Unit] = if (bool) Check.unit else fail(msg)

  private def askRaw(id: ComponentId): Run[Option[In.Component]] =
    context.map(_.in.get(id))

  private def askTyped(id: ComponentId): Run[Option[Out.Stream]] =
    context.map(_.out.get(id))

  private def putTyped(
    id: ComponentId,
    value: Out.Stream
  ): Run[Unit] =
    updateContext { ctx =>
      ctx.copy(out = ctx.out + (id -> value))
    }

  private def setEnclosing(id: Option[ComponentId]) =
    updateContext { ctx =>
      ctx.copy(enclosing = id)
    }

  private val getEnclosing =
    context.map(_.enclosing)

  private def withEnclosing[A](id: ComponentId)(nested: Run[A]): Run[A] =
    for {
      old <- getEnclosing
      _   <- setEnclosing(Some(id))
      a   <- nested
      _   <- setEnclosing(old)
    } yield a

  private def typeCheckSink(
    id: ComponentId
  ): Run[Out.Sink] = {
    import In.Component._
    withEnclosing(id) {
      askRaw(id).flatMap {
        case None      => fail(s"Component id not found")
        case Some(raw) =>
          raw match {
            case Void(sId, hint) =>
              typeCheckStream(sId, hint).map { stream =>
                Out.Sink.Void(id, stream)
              }
            case _               =>
              fail("Expected a sink")
          }
      }
    }
  }

  private def typeCheckStream(
    id: ComponentId,
    hint: Option[Type] // downstream type expectation.
  ): Run[Out.Stream] = {
    def check(comp: In.Component): Run[Out.Stream] = {
      import In.Component._
      comp match {
        case Never(elementType)                               =>
          elementType
            .orElse(hint)
            .fold[Run[Out.Stream]](
              fail("Type could not be determined. Try adding a type hint.")
            )(t => pure(Out.Stream.Never(id, t)))
        case Numbers(values)                                  =>
          pure(Out.Stream.Numbers(id, values))
        case UDF(stream, code, inputTypeHint, outputTypeHint) =>
          for {
            s      <- typeCheckStream(stream, inputTypeHint)
            // prefer provided type hint over inferred type.
            // if they do not match typechecking will fail later.
            myType <- outputTypeHint
                        .orElse(hint)
                        .fold[Run[Type]](fail("Type could not be determined. Try adding a type hint."))(pure(_))
          } yield Out.Stream.UDF(id, code, s, myType)

        case Zip(stream1, stream2, f1, f2) =>
          hint match {
            case Some(TTuple(left, right)) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
                t1 <- f1.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with left stream type"))(pure(_))
                t2 <- f2.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with right stream type"))(pure(_))
                _  <- if (t1 == t2) pass else fail("Extractor types do not match")
              } yield Out.Stream.Zip(id, s1, s2, f1, f2)
            case None                      =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
                t1 <- f1.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with left stream type"))(pure(_))
                t2 <- f2.extract(s2.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with right stream type"))(pure(_))
                _  <- if (t1 == t2) pass else fail("Extractor types do not match")
              } yield Out.Stream.Zip(id, s1, s2, f1, f2)
            case Some(t)                   =>
              fail(
                s"Found incompatible type. Expected tuple type, got $t"
              )
          }

        case InnerJoin(stream1, stream2, f1, f2) =>
          hint match {
            case Some(TTuple(left, right)) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
                t1 <- f1.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with left stream type"))(pure(_))
                t2 <- f2.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with right stream type"))(pure(_))
                _  <- if (t1 == t2) pass else fail("Extractor types do not match")
              } yield Out.Stream.InnerJoin(id, s1, s2, f1, f2)
            case None                      =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
                t1 <- f1.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with left stream type"))(pure(_))
                t2 <- f2.extract(s2.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with right stream type"))(pure(_))
                _  <- if (t1 == t2) pass else fail("Extractor types do not match")
              } yield Out.Stream.InnerJoin(id, s1, s2, f1, f2)
            case Some(t)                   =>
              fail(
                s"Found incompatible type. Expected tuple type, got $t"
              )
          }

        case LeftJoin(stream1, stream2, f1, f2) =>
          hint match {
            case Some(TTuple(left, TOption(right))) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
                t1 <- f1.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with left stream type"))(pure(_))
                t2 <- f2.extract(s2.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with right stream type"))(pure(_))
                _  <- if (t1 == t2) pass else fail("Extractor types do not match")
              } yield Out.Stream.LeftJoin(id, s1, s2, f1, f2)
            case None                               =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
                t1 <- f1.extract(s1.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with left stream type"))(pure(_))
                t2 <- f2.extract(s2.elementType)
                        .fold[Run[Type]](fail("Extractor not compatible with right stream type"))(pure(_))
                _  <- if (t1 == t2) pass else fail("Extractor types do not match")
              } yield Out.Stream.LeftJoin(id, s1, s2, f1, f2)
            case Some(t)                            =>
              fail(
                s"Found incompatible type. Expected tuple type with right side being optional, got $t"
              )
          }

        case Merge(stream1, stream2) =>
          for {
            s1 <- typeCheckStream(stream1, hint)
            s2 <- typeCheckStream(stream2, hint)
            _  <- fail(s"Found mismatched types for merge: [${s1.elementType}, ${s2.elementType}]")
                    .whenA(s1.elementType != s2.elementType)
          } yield Out.Stream.Merge(id, s1, s2)

        case MergeEither(stream1, stream2) =>
          hint match {
            case Some(TEither(left, right)) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
              } yield Out.Stream.MergeEither(id, s1, s2)
            case None                       =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
              } yield Out.Stream.MergeEither(id, s1, s2)
            case Some(t)                    =>
              fail(s"Found incompatible type. Expected either type, got $t")
          }

        case FormOutput(formId, elementType)  =>
          pure(Out.Stream.FormOutput(id, formId, elementType))
        case JFormOutput(formId, elementType) =>
          pure(Out.Stream.JFormOutput(id, formId, elementType))
        case Void(_, _)                       =>
          fail("Sink not expected here")
      }
    }

    withEnclosing(id) {
      askTyped(id).flatMap {
        case Some(stream) =>
          hint.fold(pure(stream)) { hint =>
            // unexpected type found
            if (stream.elementType != hint)
              fail(
                s"TypeError: found type ${stream.elementType} but expected type $hint"
              )
            else
              pure(stream)
          }
        case None         =>
          askRaw(id).flatMap {
            case None      => fail(s"Component id not found")
            case Some(raw) =>
              check(raw).flatTap { stream =>
                hint.fold(true)(_ == stream.elementType) match {
                  case true  => putTyped(id, stream)
                  case false =>
                    fail(
                      s"TypeError: found type ${stream.elementType} but expected type $hint"
                    )
                }
              }
          }
      }
    }
  }
}
