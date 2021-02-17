package app.forms

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class Form(
  elements: UniqueFormElements
)

object Form {

  implicit val encoder: Encoder[Form] =
    deriveEncoder

  implicit val decoder: Decoder[Form] =
    deriveDecoder
}
