package app.flows

import io.circe._

final case class ComponentId(value: String) extends AnyVal

object ComponentId {

  implicit val encoder: Encoder[ComponentId] =
    Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[ComponentId] =
    Decoder[String].map(ComponentId.apply)

  implicit val keyEncoder: KeyEncoder[ComponentId] =
    KeyEncoder[String].contramap(_.value)

  implicit val keyDecoder: KeyDecoder[ComponentId] =
    KeyDecoder[String].map(ComponentId.apply)
}
