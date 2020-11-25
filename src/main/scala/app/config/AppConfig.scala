package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

object AppConfig {
  final private[this] case class Config(http: HttpConfig.Config)

  private[this] object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  val live: ZLayer[Any, IllegalStateException, AppConfig] = {
    val all = ZLayer.fromEffect {
      ZIO
        .fromEither(ConfigSource.default.load[Config])
        .mapError(
          failures =>
            new IllegalStateException(
              s"Error loading configuration: $failures"
            )
        )
    }
    all >>> (ZLayer.fromFunction((_: Has[Config]).get.http))
  }
}
