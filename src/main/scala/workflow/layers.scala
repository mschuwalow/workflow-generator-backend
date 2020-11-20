package workflow

import config._
import zio._

object layers {

  type Layer0Env =
    AppConfig with HttpConfig

  type AppEnv = Layer0Env

  object live {
    val layer0: ZLayer[ZEnv, Throwable, Layer0Env] = AppConfig.live
    val appLayer: ZLayer[ZEnv, Throwable, AppEnv]  = layer0
  }
}
