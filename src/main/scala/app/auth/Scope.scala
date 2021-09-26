package app.auth

import doobie.postgres.circe.jsonb.implicits._
import doobie.util.{Get, Put}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

import scala.annotation.unused

sealed trait Scope

object Scope {

  case object Admin extends Scope

  final case class ForGroups(groups: Set[String]) extends Scope

  final case class ForUsers(ids: Set[String]) extends Scope

  @unused
  private implicit val configuration: Configuration =
    Configuration.default.withDiscriminator("type")

  implicit val encoder: Encoder[Scope] =
    deriveConfiguredEncoder

  implicit val decoder: Decoder[Scope] =
    deriveConfiguredDecoder

  implicit val scopeGet: Get[Scope]    =
    Get[Json].map(_.as[Scope].toOption.get)

  implicit val scopePut: Put[Scope]    =
    Put[Json].contramap(_.asJson)

}
