package app.auth

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

import scala.annotation.unused

sealed trait Scope

object Scope {

  @unused
  private implicit val configuration: Configuration =
    Configuration.default.withDiscriminator("type")

  implicit val encoder: Encoder[Scope] =
    deriveConfiguredEncoder

  implicit val decoder: Decoder[Scope] =
    deriveConfiguredDecoder

  case object Admin extends Scope

  case object Flows extends Scope

  case object Forms extends Scope

  final case class ForGroups(groups: Set[String]) extends Scope

  final case class ForUsers(ids: Set[String]) extends Scope

}
