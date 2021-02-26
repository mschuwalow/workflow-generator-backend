package app.postgres

import zio._

object DatabaseTestManager {

  trait Service {
    def managed[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A]
  }

  val live: URLayer[Database, Has[Service]] =
    ZLayer.fromFunction { env =>
      new Service {
        def managed[R, E, A](zio: ZIO[R, E, A]) = {
          val setup    = Database.migrate.orDie.provide(env)
          val teardown = Database.getFlyway.flatMap(fw => Task(fw.clean())).orDie.provide(env)
          DbLock.withPermit(ZIO.bracket(setup)(_ => teardown)(_ => zio))
        }
      }
    }

  val fake: ULayer[Has[Service]] =
    ZLayer.succeed {
      new Service {
        def managed[R, E, A](zio: ZIO[R, E, A]) =
          zio
      }
    }

  def managed[R <: Has[Service], E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.accessM(_.get.managed(zio))

  val DbLock = Runtime.default.unsafeRun(Semaphore.make(1))
}
