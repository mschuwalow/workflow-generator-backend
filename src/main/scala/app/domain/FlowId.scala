package app.domain

import io.circe._

final case class FlowId(value: String) extends AnyVal

object FlowId {

  implicit val encoder: Encoder[FlowId] =
    Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[FlowId] =
    Decoder[String].map(FlowId.apply)
}
