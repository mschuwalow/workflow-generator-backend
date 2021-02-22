package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

object HttpConfig {
  final case class Config(port: Int)

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  def const(port: Int): ULayer[HttpConfig] =
    ZLayer.succeed(Config(port))

  val get: URIO[HttpConfig, Config] =
    ZIO.access(_.get)
}
