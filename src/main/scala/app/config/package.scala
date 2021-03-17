package app

import zio._
import pureconfig._

package object config {
  type AllConfigs = Has[AppConfig] with Has[DatabaseConfig] with Has[AuthConfig] with Has[KafkaConfig]

  val configLayer: ZLayer[Any, IllegalStateException, AllConfigs] = {
    val app = ZLayer.fromEffect {
      ZIO
        .fromEither(ConfigSource.default.load[AppConfig])
        .mapError(failures =>
          new IllegalStateException(
            s"Error loading configuration: $failures"
          )
        )
    }
    app >+>
      ZLayer.fromFunction((_: Has[AppConfig]).get.http) >+>
      ZLayer.fromFunction((_: Has[AppConfig]).get.database) >+>
      ZLayer.fromFunction((_: Has[AppConfig]).get.auth) >+>
      ZLayer.fromFunction((_: Has[AppConfig]).get.kafka)
  }
}
