package app.domain

import doobie.util.meta.Meta
import io.circe._
import io.circe.generic.semiauto._
import io.circe.jawn._
import io.circe.syntax._

sealed trait FlowState

object FlowState {

  case object Running extends FlowState

  final case class Failed(reason: String) extends FlowState

  case object Done extends FlowState

  implicit val encoder: Encoder[FlowState] =
    deriveEncoder

  implicit val decoder: Decoder[FlowState] =
    deriveDecoder

  implicit val meta: Meta[FlowState] =
    Meta[String].timap(parse(_).flatMap(_.as[FlowState]).toOption.get)(
      _.asJson.noSpaces
    )
}
