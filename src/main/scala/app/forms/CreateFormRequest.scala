package app.forms

import app.auth.Scope
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class CreateFormRequest(
  elements: UniqueFormElements,
  perms: Option[Scope]
)

object CreateFormRequest {

  implicit val encoder: Encoder[CreateFormRequest] =
    deriveEncoder

  implicit val decoder: Decoder[CreateFormRequest] =
    deriveDecoder
}
