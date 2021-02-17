package app.flows

import io.circe._

import java.util.UUID

final case class FlowId(value: UUID) extends AnyVal

object FlowId {

  implicit val encoder: Encoder[FlowId] =
    Encoder[UUID].contramap(_.value)

  implicit val decoder: Decoder[FlowId] =
    Decoder[UUID].map(FlowId.apply)
}
