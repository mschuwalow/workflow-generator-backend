package app

import app.api.routes.makeRoutes
import app.config._
import cats.effect._
import fs2.Stream.Compiler._
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.clock.Clock
import zio.interop.catz._
import zio.{ExitCode => ZExitCode, _}

object Main extends App {
  type Env        = layers.AppEnv with Clock
  type AppTask[A] = RIO[Env, A]

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ZExitCode] = {
    val prog =
      for {
        httpCfg <- getHttpConfig
        httpApp  = makeRoutes[Env]
        _       <- runHttp(httpApp, httpCfg.port)
      } yield ZExitCode.success

    prog
      .provideSomeLayer[ZEnv](layers.live.appLayer)
      .orDie
  }

  def runHttp[R <: Clock](
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
