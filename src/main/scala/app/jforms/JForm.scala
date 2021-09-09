package app.jforms

import app.Type
import app.auth.Scope
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

final case class JForm(
  id: JFormId,
  dataSchema: JFormDataSchema,
  uiSchema: Json,
  perms: Option[Scope]
) {

  val outputType: Type =
    dataSchema.elementType

}

object JForm {

  implicit val encoder: Encoder[JForm] =
    deriveEncoder

  implicit val decoder: Decoder[JForm] =
    deriveDecoder

}
