package app.config

import pureconfig._
import pureconfig.error.CannotConvert
import pureconfig.generic.semiauto._
import sttp.model.Uri
import zio._

import scala.annotation.unused

object AuthConfig {
  final case class Config(studipAuthUrl: Uri)

  object Config {
    @unused
    private implicit val uriConvert: ConfigConvert[Uri] =
      ConfigConvert[String].xemap(s => Uri.parse(s).left.map(e => CannotConvert(s, "Uri", e)), _.toString())
    implicit val convert: ConfigConvert[Config]         = deriveConvert
  }

  def const(studIpAuthUrl: Uri): ULayer[AuthConfig] =
    ZLayer.succeed(Config(studIpAuthUrl))

  val get: URIO[AuthConfig, Config] =
    ZIO.access(_.get)
}
