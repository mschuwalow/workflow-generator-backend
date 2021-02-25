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
          val clean = Database.getFlyway.flatMap { flyway =>
            Task {
              flyway.clean()
              flyway.migrate()
            }
          }.orDie.provide(env)
          dbLock.withPermit(clean *> zio)
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

  private val dbLock = Runtime.default.unsafeRun(Semaphore.make(1))
}
