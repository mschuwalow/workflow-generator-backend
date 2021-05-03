package app.forms

import app.auth.Scope
import app.Type
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class Form(
  id: FormId,
  elements: UniqueFormElements,
  perms: Option[Scope]
) {
  val outputType =
    Type.TObject(elements.map(e => (e.id.value, e.elementType)))
}

object Form {

  implicit val encoder: Encoder[Form] = deriveEncoder

  implicit val decoder: Decoder[Form] = deriveDecoder

}
