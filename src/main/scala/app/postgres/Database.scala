package app.postgres

import app.config.DatabaseConfig
import cats.effect.Blocker
import doobie.hikari._
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import zio._
import zio.blocking.Blocking
import zio.interop.catz._

trait Database {
  def getTransactor: UIO[Transactor[Task]]
  def getFlyway: UIO[Flyway]
}

object Database {

  val layer: ZLayer[Has[DatabaseConfig] with Blocking, Throwable, Has[Database]] = {
    def mkFlyway(cfg: DatabaseConfig): Task[Flyway] =
      Task {
        Flyway
          .configure()
          .dataSource(cfg.url, cfg.user, cfg.password)
          .load()
      }

    def mkTransactor(
      cfg: DatabaseConfig
    ): ZManaged[Blocking, Throwable, HikariTransactor[Task]] =
      ZIO.runtime[Blocking].toManaged_.flatMap { implicit rt =>
        for {
          transactEC <- Managed.succeed(
                          rt.environment
                            .get[Blocking.Service]
                            .blockingExecutor
                            .asEC
                        )
          connectEC   = rt.platform.executor.asEC
          transactor <- HikariTransactor
                          .newHikariTransactor[Task](
                            cfg.driver,
                            cfg.url,
                            cfg.user,
                            cfg.password,
                            connectEC,
                            Blocker.liftExecutionContext(transactEC)
                          )
                          .toManaged
        } yield transactor
      }

    ZLayer.fromManaged {
      for {
        cfg        <- DatabaseConfig.get.toManaged_
        fw         <- mkFlyway(cfg).toManaged_
        transactor <- mkTransactor(cfg)
      } yield new Database {
        val getTransactor = UIO(transactor)
        val getFlyway     = UIO(fw)
      }
    }
  }

  val migrated: ZLayer[Has[DatabaseConfig] with Blocking, Throwable, Has[Database]] =
    layer.tap(_.get.getFlyway.flatMap(fw => Task(fw.migrate())))

  val getTransactor: URIO[Has[Database], Transactor[Task]] =
    ZIO.accessM(_.get.getTransactor)

  val getFlyway: URIO[Has[Database], Flyway] =
    ZIO.accessM(_.get.getFlyway)

  val migrate: RIO[Has[Database], MigrateResult] =
    getFlyway.flatMap(fw => Task(fw.migrate()))
}
