package app.postgres

import zio._

trait DatabaseTestManager {
  def managed[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A]
}

object DatabaseTestManager {

  val live: URLayer[Has[Database], Has[DatabaseTestManager]] = {
    for {
      env <- ZIO.environment[Has[Database]]
    } yield new DatabaseTestManager {
      def managed[R, E, A](zio: ZIO[R, E, A]) = {
        val setup    = Database.migrate.orDie.provide(env)
        val teardown = Database.getFlyway.flatMap(fw => Task(fw.clean())).orDie.provide(env)
        DbLock.withPermit(ZIO.bracket(setup)(_ => teardown)(_ => zio))
      }
    }
  }.toLayer

  val fake: ULayer[Has[DatabaseTestManager]] =
    ZLayer.succeed {
      new DatabaseTestManager {
        def managed[R, E, A](zio: ZIO[R, E, A]) =
          zio
      }
    }

  def managed[R <: Has[DatabaseTestManager], E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.accessM(_.get.managed(zio))

  val DbLock = Runtime.default.unsafeRun(Semaphore.make(1))
}
