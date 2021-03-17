package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

final case class AppConfig(
  http: HttpConfig,
  database: DatabaseConfig,
  auth: AuthConfig,
  kafka: KafkaConfig
)

object AppConfig {

  implicit val convert: ConfigConvert[AppConfig] = deriveConvert

  def constLayer(
    http: HttpConfig,
    database: DatabaseConfig,
    auth: AuthConfig,
    kafka: KafkaConfig
  ): ULayer[Has[AppConfig]] =
    ZLayer.succeed(AppConfig(http, database, auth, kafka))

  val get: URIO[Has[AppConfig], AppConfig] =
    ZIO.access(_.get)
}
