package app.jforms

import io.circe._

import java.util.UUID

final case class JFormId(value: UUID) extends AnyVal

object JFormId {

  implicit val encoder: Encoder[JFormId] =
    Encoder[UUID].contramap(_.value)

  implicit val decoder: Decoder[JFormId] =
    Decoder[UUID].map(JFormId.apply)
}
