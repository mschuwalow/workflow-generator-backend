package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

object KafkaConfig {
  final case class Config(bootstrapServers: List[String])

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  def const(bootstrapServers: List[String]): ULayer[KafkaConfig] =
    ZLayer.succeed(Config(bootstrapServers))

  val get: URIO[KafkaConfig, Config] =
    ZIO.access(_.get)
}
