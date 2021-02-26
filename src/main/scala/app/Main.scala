package app

import app.api.Router
import app.config._
import app.postgres.Database
import cats.effect._
import fs2.Stream.Compiler._
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.clock.Clock
import zio.internal.{Platform, Tracing}
import zio.interop.catz._
import zio.{ExitCode => ZExitCode, _}

object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ZExitCode] = {
    val prog =
      for {
        _       <- Database.migrate
        httpCfg <- HttpConfig.get
        httpApp <- Router.makeApp[AppEnvironment]
        _       <- runHttp(httpApp, httpCfg.port)
      } yield ()

    prog
      .provideSomeLayer[ZEnv](layers.prod)
      .exitCode
  }

  override val platform: Platform = Platform.default
    .withTracing(Tracing.disabled)

  private def runHttp[R <: Clock](
    httpApp: HttpApp[RIO[R, *]],
    port: Int
  ): ZIO[R, Throwable, Unit] = {
    type Task[A] = RIO[R, A]
    ZIO.runtime[R].flatMap { implicit rts =>
      BlazeServerBuilder
        .apply[Task](rts.platform.executor.asEC)
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(CORS(httpApp))
        .serve
        .compile[Task, Task, ExitCode]
        .drain
    }
  }
}
