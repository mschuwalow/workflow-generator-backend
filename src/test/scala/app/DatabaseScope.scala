package app

import app.infrastructure.postgres.Database
import zio._

trait DatabaseScope {
  def managed[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A]
}

object DatabaseScope {

  val postgres: URLayer[Has[Database], Has[DatabaseScope]] = {
    for {
      env <- ZIO.environment[Has[Database]]
    } yield new DatabaseScope {
      def managed[R, E, A](zio: ZIO[R, E, A]) = {
        val setup    = Database.migrate.orDie.provide(env)
        val teardown = Database.getFlyway.flatMap(fw => Task(fw.clean())).orDie.provide(env)
        DbLock.withPermit(ZIO.bracket(setup)(_ => teardown)(_ => zio))
      }
    }
  }.toLayer

  def around[R](before: URIO[R, _], after: URIO[R, _]): URLayer[R, Has[DatabaseScope]] =
    ZLayer.fromFunction { env =>
      new DatabaseScope {
        def managed[R1, E, A](zio: ZIO[R1, E, A]): ZIO[R1, E, A] =
          ZIO.bracket(before.provide(env))(_ => after.provide(env))(_ => zio)
      }
    }

  def before[R](setup: URIO[R, _]): URLayer[R, Has[DatabaseScope]] =
    around(setup, ZIO.unit)

  def after[R](teardown: URIO[R, _]): URLayer[R, Has[DatabaseScope]] =
    around(ZIO.unit, teardown)

  val noop: ULayer[Has[DatabaseScope]] =
    ZLayer.succeed {
      new DatabaseScope {
        def managed[R, E, A](zio: ZIO[R, E, A]) =
          zio
      }
    }

  def managed[R <: Has[DatabaseScope], E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.accessM(_.get.managed(zio))

  val DbLock = Runtime.default.unsafeRun(Semaphore.make(1))
}
