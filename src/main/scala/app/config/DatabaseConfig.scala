package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

object DatabaseConfig {

  final case class Config(url: String, driver: String, user: String, password: String)

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  def const(url: String, driver: String, user: String, password: String): ULayer[DatabaseConfig] =
    ZLayer.succeed(Config(url, driver, user, password))

  val get: URIO[DatabaseConfig, Config] =
    ZIO.access(_.get)
}
