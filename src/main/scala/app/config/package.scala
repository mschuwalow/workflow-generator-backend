package app

import zio._

package object config {
  type AppConfig      = HttpConfig with DatabaseConfig
  type HttpConfig     = Has[HttpConfig.Config]
  type DatabaseConfig = Has[DatabaseConfig.Config]
}
