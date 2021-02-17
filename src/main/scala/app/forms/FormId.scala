package app.forms

import io.circe.{Decoder, Encoder}

import java.util.UUID

final case class FormId(value: UUID) extends AnyVal

object FormId {
  implicit val encoder: Encoder[FormId] =
    Encoder[UUID].contramap(_.value)

  implicit val decoder: Decoder[FormId] =
    Decoder[UUID].map(FormId(_))
}
