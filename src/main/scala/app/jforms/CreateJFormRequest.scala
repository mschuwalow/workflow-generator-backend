package app.jforms

import app.auth.Scope
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

final case class CreateJFormRequest(
  dataSchema: JFormDataSchema,
  uiSchema: Json,
  perms: Option[Scope]
)

object CreateJFormRequest {

  implicit val encoder: Encoder[CreateJFormRequest] =
    deriveEncoder

  implicit val decoder: Decoder[CreateJFormRequest] =
    deriveDecoder

}
