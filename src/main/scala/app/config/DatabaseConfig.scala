package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

final case class DatabaseConfig(url: String, driver: String, user: String, password: String)

object DatabaseConfig {

  implicit val convert: ConfigConvert[DatabaseConfig] = deriveConvert

  def constLayer(url: String, driver: String, user: String, password: String): ULayer[Has[DatabaseConfig]] =
    ZLayer.succeed(DatabaseConfig(url, driver, user, password))

  val get: URIO[Has[DatabaseConfig], DatabaseConfig] =
    ZIO.access(_.get)
}
