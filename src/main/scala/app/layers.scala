package app

import config._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

object layers {

  type Layer0Env =
    AppConfig with HttpConfig with Logging with Blocking with Clock

  type Layer1Env =
    Layer0Env with Interpreter

  type AppEnv = Layer1Env

  object live {

    val interpreter: ZLayer[Layer0Env, Throwable, Interpreter] =
      ZLayer.identity[Layer0Env] >+> Sys.live >+> Python.live >+> UDFRunner.live(5) >+> Interpreter.stream

    val layer0: ZLayer[ZEnv, Throwable, Layer0Env] =
      ZLayer.identity[ZEnv] ++ AppConfig.live ++ Slf4jLogger.make((_, msg) => msg)

    val layer1: ZLayer[Layer0Env, Throwable, Layer1Env] =
      ZLayer.identity ++ interpreter

    val appLayer: ZLayer[ZEnv, Throwable, AppEnv] = layer0 >>> layer1
  }
}
