package app

import zio._

package object config {
  type AppConfig      = HttpConfig with DatabaseConfig
  type HttpConfig     = Has[HttpConfig.Config]
  type DatabaseConfig = Has[DatabaseConfig.Config]

  val getHttpConfig: URIO[HttpConfig, HttpConfig.Config] =
    ZIO.access(_.get)

  val getDatabaseConfig: URIO[DatabaseConfig, DatabaseConfig.Config] =
    ZIO.access(_.get)
}
