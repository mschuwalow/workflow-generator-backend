package app.domain

import io.circe._

final case class FlowId(value: Long) extends AnyVal

object FlowId {

  implicit val encoder: Encoder[FlowId] =
    Encoder[Long].contramap(_.value)

  implicit val decoder: Decoder[FlowId] =
    Decoder[Long].map(FlowId.apply)
}
