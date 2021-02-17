package app.forms

import app.flows.Type
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class FormWithId(
  id: FormId,
  elements: UniqueFormElements
) {
  val outputType =
    Type.TObject(elements.map(e => (e.id.value, e.elementType)))
}

object FormWithId {

  implicit val encoder: Encoder[FormWithId] = deriveEncoder

  implicit val decoder: Decoder[FormWithId] = deriveDecoder

}
