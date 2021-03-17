package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

final case class KafkaConfig(bootstrapServers: List[String])
object KafkaConfig {

  implicit val convert: ConfigConvert[KafkaConfig] = deriveConvert

  def const(bootstrapServers: List[String]): ULayer[Has[KafkaConfig]] =
    ZLayer.succeed(KafkaConfig(bootstrapServers))

  val get: URIO[Has[KafkaConfig], KafkaConfig] =
    ZIO.access(_.get)
}
