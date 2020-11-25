package app

import zio._

package object config {
  type AppConfig  = HttpConfig
  type HttpConfig = Has[HttpConfig.Config]

  val getHttpConfig: URIO[HttpConfig, HttpConfig.Config] =
    ZIO.access(_.get)
}
