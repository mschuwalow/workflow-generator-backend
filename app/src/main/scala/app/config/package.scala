package app

import zio._

package object config {
  type AppConfig      = HttpConfig with DatabaseConfig with AuthConfig
  type HttpConfig     = Has[HttpConfig.Config]
  type DatabaseConfig = Has[DatabaseConfig.Config]
  type AuthConfig     = Has[AuthConfig.Config]
}
