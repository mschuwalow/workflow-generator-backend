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
    Layer0Env with Sys

  type Layer2Env =
    Layer1Env with Python

  type Layer3Env =
    Layer2Env with UDFRunner

  type AppEnv = Layer3Env

  object live {

    val layer0: ZLayer[ZEnv, Throwable, Layer0Env] =
      ZLayer.identity[ZEnv] ++ AppConfig.live ++ Slf4jLogger.make((_, msg) => msg)

    val layer1: ZLayer[Layer0Env, Nothing, Layer1Env] =
      ZLayer.identity ++ Sys.live

    val layer2: ZLayer[Layer1Env, Nothing, Layer2Env] =
      ZLayer.identity ++ Python.live

    val layer3: ZLayer[Layer2Env, Throwable, Layer3Env] =
      ZLayer.identity ++ UDFRunner.live(5)

    val appLayer: ZLayer[ZEnv, Throwable, AppEnv] = layer0 >>> layer1 >>> layer2 >>> layer3
  }
}
