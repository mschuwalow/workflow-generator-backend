package app.flows.compiler

import cats.Monad

sealed trait Check[S, +E, +A] { self =>
  import Check._

  def run(state: S): Either[E, (S, A)]

  def map[B](f: A => B): Check[S, E, B] =
    flatMap(a => Done(f(a)))

  def flatMap[E1 >: E, B](f: A => Check[S, E1, B]): Check[S, E1, B] =
    FlatMap(self, f)

  def isolated: Check[S, E, A] =
    for {
      s <- getState
      a <- self
      _ <- setState(s)
    } yield a
}

object Check {

  final case class Fail[S, E](e: E) extends Check[S, E, Nothing] {
    def run(state: S) =
      Left(e)
  }

  final case class Done[S, A](a: A) extends Check[S, Nothing, A] {
    def run(state: S) =
      Right((state, a))
  }

  final case class Get[S]() extends Check[S, Nothing, S] {
    def run(state: S) =
      Right((state, state))
  }

  final case class Set[S](s: S) extends Check[S, Nothing, Unit] {
    def run(state: S) =
      Right((s, ()))
  }

  final case class FlatMap[S, E, A, B](self: Check[S, E, A], cont: A => Check[S, E, B]) extends Check[S, E, B] {
    def run(state: S) =
      self.run(state).flatMap { case (s, a) =>
        cont(a).run(s)
      }
  }

  implicit def monad[S, E]: Monad[Check[S, E, *]] =
    new Monad[Check[S, E, *]] {
      def flatMap[A, B](fa: Check[S, E, A])(f: A => Check[S, E, B]) =
        fa.flatMap(f)

      def tailRecM[A, B](a: A)(f: A => Check[S, E, Either[A, B]]) =
        flatMap(f(a)) {
          case Left(a)  => tailRecM(a)(f)
          case Right(b) => pure(b)
        }

      def pure[A](x: A) =
        Done(x)

    }

  def fail[S, E](e: E): Check[S, E, Nothing] =
    Fail(e)

  def done[S, A](a: A): Check[S, Nothing, A] =
    Done(a)

  def getState[S]: Check[S, Nothing, S] =
    Get()

  def setState[S, A](s: S): Check[S, Nothing, Unit] =
    Set(s)

  def unit[S]: Check[S, Nothing, Unit] =
    done(())

  def updateState[S](f: S => S): Check[S, Nothing, Unit] =
    for {
      s <- getState
      _ <- setState(f(s))
    } yield ()

}
