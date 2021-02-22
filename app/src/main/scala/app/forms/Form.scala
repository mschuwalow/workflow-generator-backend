package app.forms

import app.auth.Scope
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class Form(
  elements: UniqueFormElements,
  perms: Option[Scope]
)

object Form {

  implicit val encoder: Encoder[Form] =
    deriveEncoder

  implicit val decoder: Decoder[Form] =
    deriveDecoder
}
