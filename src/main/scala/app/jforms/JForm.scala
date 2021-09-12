package app.jforms

import app.Type
import app.auth.Scope
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
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
    Encoder.instance { form =>
      deriveEncoder[JForm]
        .mapJsonObject(_.add("outputType", form.outputType.asJson))
        .apply(form)
    }

  implicit val decoder: Decoder[JForm] =
    deriveDecoder

}
