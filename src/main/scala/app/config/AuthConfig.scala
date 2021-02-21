package app.config

import pureconfig._
import pureconfig.generic.semiauto._
import zio._
import sttp.model.Uri
import pureconfig.error.CannotConvert
import scala.annotation.unused

object AuthConfig {
  final case class Config(studIpAuthUrl: Uri)

  object Config {
    @unused
    private implicit val uriConvert: ConfigConvert[Uri] =
      ConfigConvert[String].xemap(s => Uri.parse(s).left.map(e => CannotConvert(s, "Uri", e)), _.toString())
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  def const(studIpAuthUrl: Uri): ULayer[AuthConfig] =
    ZLayer.succeed(Config(studIpAuthUrl))

  val get: URIO[AuthConfig, Config] =
    ZIO.access(_.get)
}
