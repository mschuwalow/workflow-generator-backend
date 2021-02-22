package app.forms

import io.circe.{Decoder, Encoder}

final case class FormElementId(value: String) extends AnyVal

object FormElementId {

  implicit val encoder: Encoder[FormElementId] =
    Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[FormElementId] =
    Decoder[String].map(FormElementId(_))

}
