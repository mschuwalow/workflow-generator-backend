package app.jforms

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

final case class CreateJFormRequest(
  dataSchema: JFormDataSchema,
  uiSchema: Json
)

object CreateJFormRequest {

  implicit val encoder: Encoder[CreateJFormRequest] =
    deriveEncoder

  implicit val decoder: Decoder[CreateJFormRequest] =
    deriveDecoder

}
