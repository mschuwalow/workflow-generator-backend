package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

object AppConfig {

  final private case class Config(
    http: HttpConfig.Config,
    database: DatabaseConfig.Config,
    auth: AuthConfig.Config,
    kafka: KafkaConfig.Config
  )

  private object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  val live: ZLayer[Any, IllegalStateException, AppConfig] = {
    val all = ZLayer.fromEffect {
      ZIO
        .fromEither(ConfigSource.default.load[Config])
        .mapError(failures =>
          new IllegalStateException(
            s"Error loading configuration: $failures"
          )
        )
    }
    all >>> (
      ZLayer.fromFunction((_: Has[Config]).get.http) ++
        ZLayer.fromFunction((_: Has[Config]).get.database) ++
        ZLayer.fromFunction((_: Has[Config]).get.auth) ++
        ZLayer.fromFunction((_: Has[Config]).get.kafka)
    )
  }
}
