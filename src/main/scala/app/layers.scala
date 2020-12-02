package app

import config._
import zio._
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

object layers {

  type Layer0Env =
    AppConfig with HttpConfig with Logging

  type AppEnv = Layer0Env

  object live {

    val layer0: ZLayer[ZEnv, Throwable, Layer0Env] =
      AppConfig.live ++ Slf4jLogger.make((_, msg) => msg)
    val appLayer: ZLayer[ZEnv, Throwable, AppEnv] = layer0
  }
}
