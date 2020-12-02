package app.compiler

import app.compiler.Type._
import cats.Monad
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.traverse._

object semantic {

  def typecheck(graph: raw.Graph): Either[String, typed.Flow] = {
    // all starting points to traverse the graph
    val sinks = graph.nodes.collect { case (id, _: raw.Sink) =>
      id
    }.toList

    val checkResult = Transform.context.flatMap { ctx =>
      // we don't rewrite graphs in this phase
      Transform.require(
        (ctx.in.size - sinks.size) == ctx.out.size,
        "Not all components were connected to sinks."
      )
    }

    (sinks.traverse(typeCheckSink(_)) <* checkResult)
      .run(Context.initial(graph.nodes))
      .map { case (_, sinks) =>
        typed.Flow(sinks)
      }
  }

  final private[compiler] case class Context(
    in: Map[ComponentId, raw.Component],
    out: Map[ComponentId, typed.Stream],
    enclosing: Option[ComponentId])

  private[compiler] object Context {

    def initial(in: Map[ComponentId, raw.Component]): Context =
      Context(in, Map.empty, None)
  }

  // state monad with error collection
  private[compiler] trait Transform[+A] { self =>
    import Transform._
    def run(ctx: Context): Either[String, (Context, A)]

    def map[B](f: A => B): Transform[B] =
      transform {
        run(_).map { case (ctx, a) => (ctx, f(a)) }
      }

    def flatMap[B](f: A => Transform[B]) =
      transform {
        run(_).flatMap { case (ctx, a) =>
          f(a).run(ctx)
        }
      }
  }

  private[compiler] object Transform {

    val unit: Transform[Unit] = transform { ctx =>
      Right((ctx, ()))
    }

    val context: Transform[Context] = transform { ctx =>
      Right((ctx, ctx))
    }

    def fail(msg: String): Transform[Nothing] =
      transform { ctx =>
        Left(s"${ctx.enclosing.fold("")(id => s"[${id.value}]: ")}$msg")
      }

    def require(
      bool: Boolean,
      msg: String
    ): Transform[Unit] = if (bool) unit else fail(msg)

    def askRaw(id: ComponentId): Transform[Option[raw.Component]] =
      context.map(_.in.get(id))

    def askTyped(id: ComponentId): Transform[Option[typed.Stream]] =
      context.map(_.out.get(id))

    def putTyped(
      id: ComponentId,
      value: typed.Stream
    ): Transform[Unit] =
      transform { ctx =>
        Right((ctx.copy(out = ctx.out + (id -> value)), ()))
      }

    def setEnclosing(id: Option[ComponentId]): Transform[Unit] =
      transform { ctx =>
        Right((ctx.copy(enclosing = id), ()))
      }

    val getEnclosing: Transform[Option[ComponentId]] = transform { ctx =>
      Right((ctx, ctx.enclosing))
    }

    def withEnclosing[A](id: ComponentId)(nested: Transform[A]): Transform[A] =
      for {
        old <- getEnclosing
        _   <- setEnclosing(Some(id))
        a   <- nested
        _   <- setEnclosing(old)
      } yield a

    def pure[A](a: => A): Transform[A] =
      transform { ctx =>
        Right((ctx, a))
      }

    def transform[A](f: Context => Either[String, (Context, A)]) =
      new Transform[A] {
        def run(ctx: Context): Either[String, (Context, A)] = f(ctx)
      }

    implicit val monad: Monad[Transform] =
      new Monad[Transform] {

        override def flatMap[A, B](
          fa: Transform[A]
        )(
          f: A => Transform[B]
        ): Transform[B] = fa.flatMap(f)

        override def tailRecM[A, B](
          a: A
        )(
          f: A => Transform[Either[A, B]]
        ): Transform[B] =
          f(a).flatMap {
            case Left(a)  => tailRecM(a)(f)
            case Right(b) => pure(b)
          }

        override def pure[A](x: A): Transform[A] = Transform.pure(x)
      }
  }
  import Transform._

  private[compiler] def typeCheckSink(
    id: ComponentId
  ): Transform[typed.Sink] = {
    import raw._
    withEnclosing(id) {
      askRaw(id).flatMap {
        case None => fail(s"Component id not found")
        case Some(raw) =>
          raw match {
            case Sink.Void(sId, hint) =>
              typeCheckStream(sId, hint).map { stream =>
                typed.Void(id, stream)
              }
            case _ =>
              fail("Expected a sink")
          }
      }
    }
  }

  private[compiler] def typeCheckStream(
    id: ComponentId,
    hint: Option[Type] // downstream type expectation.
  ): Transform[typed.Stream] = {
    def check(comp: raw.Component): Transform[typed.Stream] = {
      import raw._
      comp match {
        case Source.Never(elementType) =>
          elementType
            .orElse(hint)
            .fold[Transform[typed.Stream]](
              fail("Type could not be determined. Try adding a type hint.")
            )(t => pure(typed.Never(id, t)))
        case Source.Numbers(values) =>
          pure(typed.Numbers(id, values))
        case Transformer1.UDF(stream, code, inputTypeHint, outputTypeHint) =>
          for {
            s <- typeCheckStream(stream, inputTypeHint)
            // prefer provided type hint over inferred type.
            // if they do not match typechecking will fail later.
            myType <-
              outputTypeHint
                .orElse(hint)
                .fold[Transform[Type]](
                  fail(
                    "Type could not be determined. Try adding a type hint"
                  )
                )(pure(_))
          } yield typed.UDF1(id, code, s, myType)
        case Transformer2.InnerJoin(stream1, stream2) =>
          hint match {
            case Some(TTuple(left, right)) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
              } yield typed.InnerJoin(id, s1, s2)
            case None =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
              } yield typed.InnerJoin(id, s1, s2)
            case Some(t) =>
              fail(s"Found incompatible type. Expected tuple type, got $t")
          }
        case Transformer2.LeftJoin(stream1, stream2) =>
          hint match {
            case Some(TTuple(left, TOption(right))) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
              } yield typed.LeftJoin(id, s1, s2)
            case None =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
              } yield typed.LeftJoin(id, s1, s2)
            case Some(t) =>
              fail(
                s"Found incompatible type. Expected tuple type with right side being optional, got $t"
              )
          }
        case Transformer2.Merge(stream1, stream2) =>
          hint match {
            case Some(TEither(left, right)) =>
              for {
                s1 <- typeCheckStream(stream1, Some(left))
                s2 <- typeCheckStream(stream2, Some(right))
              } yield typed.Merge(id, s1, s2)
            case None =>
              for {
                s1 <- typeCheckStream(stream1, None)
                s2 <- typeCheckStream(stream2, None)
              } yield typed.Merge(id, s1, s2)
            case Some(t) =>
              fail(s"Found incompatible type. Expected either type, got $t")
          }
        case Transformer2.UDF(
              stream1,
              stream2,
              input1TypeHint,
              input2TypeHint,
              outputTypeHint
            ) =>
          for {
            s1 <- typeCheckStream(stream1, input1TypeHint)
            s2 <- typeCheckStream(stream2, input2TypeHint)
            // prefer provided type hint over inferred type.
            // if they do not match typechecking will fail later.
            myType <-
              outputTypeHint
                .orElse(hint)
                .fold[Transform[Type]](
                  fail(
                    "Type could not be determined. Try adding a type hint"
                  )
                )(pure(_))
          } yield typed.UDF2(id, s1, s2, myType)
        case _: Sink =>
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
        case None =>
          askRaw(id).flatMap {
            case None => fail(s"Component id not found")
            case Some(raw) =>
              check(raw).flatTap { stream =>
                hint.fold(true)(_ == stream.elementType) match {
                  case true => putTyped(id, stream)
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
