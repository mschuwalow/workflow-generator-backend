package app

import app.repository.WorkflowRepository
import config._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

object layers {

  type AppEnv = HttpConfig
    with Logging
    with Clock
    with Interpreter
    with WorkflowManager
    with WorkflowRepository

  object live {

    type Layer0Env =
      AppConfig with HttpConfig with Logging with Blocking with Clock

    type Layer1Env =
      Layer0Env with Interpreter with Database

    type Layer2Env =
      Layer1Env with WorkflowRepository

    val interpreter: ZLayer[Layer0Env, Throwable, Interpreter] =
      ZLayer.identity[Layer0Env] >+> Sys.live >+> Python.live >+> UDFRunner.live(
        5
      ) >+> Interpreter.stream

    val layer0: ZLayer[ZEnv, Throwable, Layer0Env] =
      ZLayer.identity[ZEnv] ++ AppConfig.live ++ Slf4jLogger.make((_, msg) => msg)

    val layer1: ZLayer[Layer0Env, Throwable, Layer1Env] =
      ZLayer.identity[Layer0Env] ++ interpreter ++ Database.live

    val layer2: ZLayer[Layer1Env, Nothing, Layer2Env] =
      ZLayer.identity[Layer1Env] ++ WorkflowRepository.doobie

    val layer3: ZLayer[Layer2Env, Throwable, AppEnv] =
      ZLayer.identity[Layer2Env] ++ WorkflowManager.live

    val appLayer: ZLayer[ZEnv, Throwable, AppEnv] =
      layer0 >>> layer1 >>> layer2 >>> layer3
  }
}
