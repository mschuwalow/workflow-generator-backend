package app.backend

import app.backend.nodes._
import app.backend.Type._
import cats.Monad
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.list._
import app.backend.nodes.TransformerOp1.Identity
import app.backend.nodes.TransformerOp1.UDF

object semantic {

  def typecheck(in: Map[ComponentId, raw.Component]): Either[String, Map[ComponentId, typed.Component]] = {
    // all starting points to traverse the graph
    val sinks = in.collect {
      case (id, raw.Sink(_, _)) => id
     }.toList
    sinks.traverse(typeCheckComponentId).run(Context.initial(in)).map(_._1.out)
  }

  private[backend] final case class Context(
    in: Map[ComponentId, raw.Component],
    out: Map[ComponentId, typed.Component]
  )

  private[backend] object Context {
    def initial(in: Map[ComponentId, raw.Component]): Context =
      Context(in, Map.empty)
  }

  // state monad with error collection
  private[backend] trait Transform[+A] { self =>
    import Transform._
    def run(ctx: Context): Either[String, (Context, A)]

    def map[B](f: A => B): Transform[B] =
      transform {
        run(_).map { case (ctx, a) => (ctx, f(a)) }
      }

    def flatMap[B](f: A => Transform[B]) = transform {
      run(_).flatMap { case (ctx, a) =>
        f(a).run(ctx)
      }
    }
  }

  private[backend] object Transform {
    val unit: Transform[Unit] = transform { ctx =>
      Right((ctx, ()))
    }
    def fail(msg: String): Transform[Nothing] = transform { _ =>
      Left(msg)
    }
    def askRaw(id: ComponentId): Transform[Option[raw.Component]] = transform { ctx =>
      Right((ctx, ctx.in.get(id)))
    }
    def askTyped(id: ComponentId): Transform[Option[typed.Component]] = transform { ctx =>
      Right((ctx, ctx.out.get(id)))
    }
    def putTyped(id: ComponentId, value: typed.Component): Transform[Unit] = transform { ctx =>
      Right((ctx.copy(out = ctx.out + (id -> value)), ()))
    }
    def pure[A](a: => A): Transform[A] = transform { ctx =>
      Right((ctx, a))
    }
    def transform[A](f: Context => Either[String, (Context, A)]) =
      new Transform[A] {
        def run(ctx: Context): Either[String,(Context, A)] = f(ctx)
      }

    implicit val monad: Monad[Transform] =
      new Monad[Transform] {

        override def flatMap[A, B](fa: Transform[A])(f: A => Transform[B]): Transform[B] = fa.flatMap(f)

        override def tailRecM[A, B](a: A)(f: A => Transform[Either[A,B]]): Transform[B] = f(a).flatMap {
          case Left(a) => tailRecM(a)(f)
          case Right(b) => pure(b)
        }

        override def pure[A](x: A): Transform[A] = pure(x)

      }
  }
  import Transform.{askRaw, askTyped, putTyped, fail, pure}

  private[backend] def typeCheckComponentId(id: ComponentId, hint: Option[Type]): Transform[typed.Component] =
    askTyped(id).flatMap {
      case Some(component) => hint.fold(pure(component)) { hint =>
        // unexpected type found
        if (component.ctype != hint) {
          fail(s"TypeError: [$id] found type ${component.ctype} but expected type $hint")
        } else {
          pure(component)
        }
      }
      case None =>
        askRaw(id).flatMap {
          case None => fail(s"Component id ${id.value} not found")
          case Some(raw) => typeCheckComponent(id, raw, hint)
        }
    }

  private[backend] def typeCheckComponent(id: ComponentId, comp: raw.Component, hint: Option[Type]): Transform[typed.Component] =
    comp match {
      case raw.Source(sourceOp) =>
        for {
          typedComp <- typeCheckSource(sourceOp)
          _ <- putTyped(id, typedComp)
        } yield typedComp
      case raw.Sink(streamId, sinkOp) => ???
        typeCheckComponentId(streamId, Some(sinkOp.inType)).flatMap { stream =>
          if (stream.ctype == sinkOp.inType) {
            // well typed sink
            val typedComp = typed.Sink(streamId, sinkOp)
            putTyped(id, typedComp).as(typedComp)
          } else {
            fail(s"TypeError: [$id] expected type ${sinkOp.inType} but got type ${stream.ctype}")
          }
        }
      case raw.Transformer1(sId, op) =>
        for {
          streamHint <- inferTransformer1Hint(op, hint)
          stream    <- typeCheckComponentId(sId, streamHint)
          typedComp <- typeCheckTransformer1(stream, op)
          _         <- putTyped(id, typedComp)
        } yield typedComp
      case raw.Transformer2(sId1, sId2, op) =>
        for {
          s1 <- typeCheckComponentId(sId1, None)
          s2 <- typeCheckComponentId(sId2, None)
          checked <- typeCheckTransformer2(s1, s2, sId1, sId2, op)
          _ <- putTyped(id, checked)
        } yield checked
      case raw.Dummy => ???
    }

  private[backend] def typeCheckSource(
    op: SourceOp
  ): Transform[typed.Source] =
    pure {
      typed.Source(op, op.outType)
    }

  private[backend] def typeCheckTransformer1(
    stream: typed.Component,
    op: TransformerOp1
  ): Transform[typed.Component] = {
    import TransformerOp1._
    op match {
      case Identity =>
        // this node can be eliminated
        pure(stream)
      case UDF(sHint) => ???
    }
  }

  private[backend] def typeCheckTransformer2(
    s1: typed.Component,
    s2: typed.Component,
    s1Id: ComponentId,
    s2Id: ComponentId,
    op: TransformerOp2
  ): Transform[typed.Component] = {
    import TransformerOp2._
    for {
      _ <- if (s1.isSink || s2.isSink) fail("Sinks can only occur at the end of a graph") else Transform.unit
      outtype = op match {
        case LeftJoin => tTuple(s1.ctype, tOption(s2.ctype))
        case InnerJoin => tTuple(s1.ctype, s2.ctype)
      }
    } yield typed.Transformer2(s1Id, s2Id, op, outtype)
  }

  // tries to propagate a type hint upward in order to help type inference
  private[backend] def inferTransformer1Hint(
    op: TransformerOp1,
    hint: Option[Type]
  ): Transform[Option[Type]] =
    op match {
      case Identity => pure(hint)
      case UDF(sHint) => pure(sHint)
    }
}
