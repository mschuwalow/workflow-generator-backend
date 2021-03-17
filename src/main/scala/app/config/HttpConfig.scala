package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._

final case class HttpConfig(port: Int)

object HttpConfig {

  implicit val convert: ConfigConvert[HttpConfig] = deriveConvert

  def const(port: Int): ULayer[Has[HttpConfig]] =
    ZLayer.succeed(HttpConfig(port))

  val get: URIO[Has[HttpConfig], HttpConfig] =
    ZIO.access(_.get)
}
