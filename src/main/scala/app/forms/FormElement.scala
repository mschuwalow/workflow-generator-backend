package app.forms

import app.Type
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}

import scala.annotation.unused

sealed trait FormElement {
  def id: FormElementId
  def elementType: Type
}

object FormElement {

  final case class TextField(
    id: FormElementId,
    label: String
  ) extends FormElement {
    val elementType = Type.TString
  }

  final case class NumberField(
    id: FormElementId,
    label: String
  ) extends FormElement {
    val elementType = Type.TNumber
  }

  final case class DatePicker(
    id: FormElementId,
    label: String
  ) extends FormElement {
    val elementType = Type.TDate
  }

  @unused
  private implicit val configuration: Configuration =
    Configuration.default.withDiscriminator("type")

  implicit val encoder: Encoder[FormElement] =
    deriveConfiguredEncoder

  implicit val decoder: Decoder[FormElement] =
    deriveConfiguredDecoder
}
