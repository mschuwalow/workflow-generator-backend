package app.compiler

import cats.Monad
import cats.instances.either._
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.traverse._

object syntactic {

  def checkCycles(graph: raw.Graph): Either[String, raw.Graph] = {
    // all starting points to traverse the graph
    val sinks = graph.nodes.collect {
      case (id, _: raw.Sink) => id
    }.toList
    sinks.traverse { id =>
      // every subgraph needs to be checked seperately
      checkComponent(id).run(Context.initial(graph.nodes))
    }.as(graph)
  }

  final private[compiler] case class Context(
    nodes: Map[ComponentId, raw.Component],
    visited: Set[ComponentId],
    position: List[ComponentId])

  object Context {

    def initial(nodes: Map[ComponentId, raw.Component]): Context =
      Context(nodes, Set.empty, Nil)
  }

  // another state monad with error handling
  private[compiler] trait Check[+A] { self =>
    import Check._

    def run(ctx: Context): Either[String, (Context, A)]

    def map[B](f: A => B): Check[B] = check {
      run(_).map { case (ctx, a) => (ctx, f(a)) }
    }

    def flatMap[B](f: A => Check[B]) = check {
      run(_).flatMap {
        case (ctx, a) =>
          f(a).run(ctx)
      }
    }
  }

  private[compiler] object Check {
    val unit: Check[Unit] = pure(())

    def getComponent(id: ComponentId): Check[raw.Component] =
      getContext.flatMap { ctx =>
        ctx.nodes
          .get(id)
          .fold[Check[raw.Component]](
            fail(s"Component with id ${id.value} not found")
          )(
            pure
          )
      }

    val getContext: Check[Context] = check { ctx =>
      Right((ctx, ctx))
    }

    def updateContext(f: Context => Context): Check[Context] = check { ctx =>
      val next = f(ctx)
      Right((next, next))
    }

    def addVisisted(id: ComponentId): Check[Unit] =
      getContext.flatMap { ctx =>
        if (ctx.visited.contains(id)) {
          fail("Cycle detected")
        } else {
          updateContext(ctx => ctx.copy(visited = ctx.visited + id)).void
        }
      }

    def withPosition[A](id: ComponentId)(nested: Check[A]): Check[A] =
      for {
        _ <- updateContext(old => old.copy(position = id +: old.position))
        a <- nested
        _ <- updateContext(old => old.copy(position = old.position.tail))
      } yield a

    def fail(msg: String): Check[Nothing] = check { ctx =>
      Left(
        s"Failed while checking ${ctx.position.reverse.map(_.value).mkString("->")}: $msg"
      )
    }

    def check[A](f: Context => Either[String, (Context, A)]): Check[A] =
      new Check[A] {

        def run(ctx: Context): Either[String, (Context, A)] =
          f(ctx)
      }

    def pure[A](a: A): Check[A] = check { ctx =>
      Right((ctx, a))
    }

    implicit val monad: Monad[Check] = new Monad[Check] {

      def flatMap[A, B](fa: Check[A])(f: A => Check[B]): Check[B] =
        fa.flatMap(f)

      def tailRecM[A, B](a: A)(f: A => Check[Either[A, B]]): Check[B] =
        f(a).flatMap {
          case Left(a)  => tailRecM(a)(f)
          case Right(b) => pure(b)
        }

      def pure[A](x: A): Check[A] = Check.pure(x)
    }
  }
  import Check._

  private[compiler] def checkComponent(id: ComponentId): Check[Unit] = {
    import raw._
    withPosition(id) {
      addVisisted(id) *> getComponent(id).flatMap {
        case Source.Never(_)                => unit
        case Transformer1.UDF(stream, _, _) => checkComponent(stream)
        case Transformer2.InnerJoin(stream1, stream2) =>
          checkComponent(stream1) *> checkComponent(stream2)
        case Transformer2.LeftJoin(stream1, stream2) =>
          checkComponent(stream1) *> checkComponent(stream2)
        case Transformer2.Merge(stream1, stream2) =>
          checkComponent(stream1) *> checkComponent(stream2)
        case Transformer2.UDF(stream1, stream2, _, _, _) =>
          checkComponent(stream1) *> checkComponent(stream2)
        case Sink.Void(stream, _) =>
          checkComponent(stream)
      }
    }
  }
}
