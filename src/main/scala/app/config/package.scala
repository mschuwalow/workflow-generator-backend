package app

import zio._

package object config {
  type AppConfig      = HttpConfig with DatabaseConfig with AuthConfig with KafkaConfig
  type HttpConfig     = Has[HttpConfig.Config]
  type DatabaseConfig = Has[DatabaseConfig.Config]
  type AuthConfig     = Has[AuthConfig.Config]
  type KafkaConfig    = Has[KafkaConfig.Config]
}
