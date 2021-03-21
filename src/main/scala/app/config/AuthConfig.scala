package app.config

import pureconfig._
import pureconfig.error.CannotConvert
import pureconfig.generic.semiauto._
import sttp.model.Uri
import zio._

import scala.annotation.unused

final case class AuthConfig(studipAuthUrl: Uri, adminUsers: Set[String])

object AuthConfig {
  @unused
  private implicit val uriConvert: ConfigConvert[Uri] =
    ConfigConvert[String].xemap(s => Uri.parse(s).left.map(e => CannotConvert(s, "Uri", e)), _.toString())

  implicit val convert: ConfigConvert[AuthConfig] = deriveConvert

  def constLayer(studIpAuthUrl: Uri, adminUsers: Set[String]): ULayer[Has[AuthConfig]] =
    ZLayer.succeed(AuthConfig(studIpAuthUrl, adminUsers))

  val get: URIO[Has[AuthConfig], AuthConfig] =
    ZIO.access(_.get)

}
