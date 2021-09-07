package app.jforms

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

final case class JForm(
  id: JFormId,
  dataSchema: JFormDataSchema,
  uiSchema: Json
)

object JForm {

  implicit val encoder: Encoder[JForm] =
    deriveEncoder

  implicit val decoder: Decoder[JForm] =
    deriveDecoder

}
