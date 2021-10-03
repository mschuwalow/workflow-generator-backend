package app.flows.inbound.compiler

import app.Type._
import app.flows.{ComponentId, resolved, typed}
import app.{Error, Type}
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.traverse._

private[inbound] object semantic {

  def typecheck(graph: resolved.CreateFlowRequest): Either[Error.GraphValidationFailed, typed.CreateFlowRequest] = {
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
        typed.CreateFlowRequest(sinks)
      }

    result.left.map(Error.GraphValidationFailed(_))
  }

  private[semantic] type Run[A] = Check[Context, String, A]

  final private[semantic] case class Context(
    in: Map[ComponentId, resolved.Component],
    out: Map[ComponentId, typed.Stream],
    enclosing: Option[ComponentId]
  )

  private[semantic] object Context {

    def initial(in: Map[ComponentId, resolved.Component]): Context =
      Context(in, Map.empty, None)
  }

  private[semantic] val context: Run[Context] =
    Check.getState[Context]

  private[semantic] def updateContext(f: Context => Context) =
    Check.updateState(f)

  private[semantic] def fail(msg: String): Run[Nothing] =
    context.flatMap { ctx =>
      Check.fail(s"${ctx.enclosing.fold("")(id => s"[${id.value}]: ")}$msg")
    }

  private[semantic] def pure[A](a: A): Run[A] =
    Check.done(a)

  private[semantic] def require(
    bool: Boolean,
    msg: String
  ): Run[Unit] = if (bool) Check.unit else fail(msg)

  private[semantic] def askRaw(id: ComponentId): Run[Option[resolved.Component]] =
    context.map(_.in.get(id))

  private[semantic] def askTyped(id: ComponentId): Run[Option[typed.Stream]] =
    context.map(_.out.get(id))

  private[semantic] def putTyped(
    id: ComponentId,
    value: typed.Stream
  ): Run[Unit] =
    updateContext { ctx =>
      ctx.copy(out = ctx.out + (id -> value))
    }

  private[semantic] def setEnclosing(id: Option[ComponentId]) =
    updateContext { ctx =>
      ctx.copy(enclosing = id)
    }

  private[semantic] val getEnclosing =
    context.map(_.enclosing)

  private[semantic] def withEnclosing[A](id: ComponentId)(nested: Run[A]): Run[A] =
    for {
      old <- getEnclosing
      _   <- setEnclosing(Some(id))
      a   <- nested
      _   <- setEnclosing(old)
    } yield a

  private[semantic] def typeCheckSink(
    id: ComponentId
  ): Run[typed.Sink] = {
    import resolved.Component._
    withEnclosing(id) {
      askRaw(id).flatMap {
        case None      => fail(s"Component id not found")
        case Some(raw) =>
          raw match {
            case Void(sId, hint) =>
              typeCheckStream(sId, hint).map { stream =>
                typed.Sink.Void(id, stream)
              }
            case _               =>
              fail("Expected a sink")
          }
      }
    }
  }

  private[semantic] def typeCheckStream(
    id: ComponentId,
    hint: Option[Type] // downstream type expectation.
  ): Run[typed.Stream] = {
    def check(comp: resolved.Component): Run[typed.Stream] = {
      import resolved.Component._
      comp match {
        case Never(elementType)                               =>
          elementType
            .orElse(hint)
            .fold[Run[typed.Stream]](
              fail("Type could not be determined. Try adding a type hint.")
            )(t => pure(typed.Stream.Never(id, t)))
        case Numbers(values)                                  =>
          pure(typed.Stream.Numbers(id, values))
        case UDF(stream, code, inputTypeHint, outputTypeHint) =>
          for {
            s      <- typeCheckStream(stream, inputTypeHint)
            // prefer provided type hint over inferred type.
            // if they do not match typechecking will fail later.
            myType <- outputTypeHint
                        .orElse(hint)
                        .fold[Run[Type]](fail("Type could not be determined. Try adding a type hint."))(pure(_))
          } yield typed.Stream.UDF(id, code, s, myType)
        case InnerJoin(stream1, stream2)                      =>
          hint match {
            case Some(TTuple(left, right)) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
              } yield typed.Stream.InnerJoin(id, s1, s2)
            case None                      =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
              } yield typed.Stream.InnerJoin(id, s1, s2)
            case Some(t)                   =>
              fail(s"Found incompatible type. Expected tuple type, got $t")
          }
        case LeftJoin(stream1, stream2)                       =>
          hint match {
            case Some(TTuple(left, TOption(right))) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
              } yield typed.Stream.LeftJoin(id, s1, s2)
            case None                               =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
              } yield typed.Stream.LeftJoin(id, s1, s2)
            case Some(t)                            =>
              fail(
                s"Found incompatible type. Expected tuple type with right side being optional, got $t"
              )
          }
        case Merge(stream1, stream2)                          =>
          hint match {
            case Some(TEither(left, right)) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
              } yield typed.Stream.Merge(id, s1, s2)
            case None                       =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
              } yield typed.Stream.Merge(id, s1, s2)
            case Some(t)                    =>
              fail(s"Found incompatible type. Expected either type, got $t")
          }
        case FormOutput(formId, elementType)                  =>
          pure(typed.Stream.FormOutput(id, formId, elementType))
        case JFormOutput(formId, elementType)                 =>
          pure(typed.Stream.JFormOutput(id, formId, elementType))
        case Void(_, _)                                       =>
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
