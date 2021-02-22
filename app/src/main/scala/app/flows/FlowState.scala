package app.flows

import doobie.util.meta.Meta
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.jawn._
import io.circe.syntax._

import scala.annotation.unused

sealed trait FlowState

object FlowState {

  case object Running extends FlowState

  final case class Failed(reason: String) extends FlowState

  case object Done extends FlowState

  @unused
  private implicit val configuration: Configuration =
    Configuration.default.withDiscriminator("type")

  implicit val encoder: Encoder[FlowState] =
    deriveConfiguredEncoder

  implicit val decoder: Decoder[FlowState] =
    deriveConfiguredDecoder

  implicit val meta: Meta[FlowState] =
    Meta[String].timap(parse(_).flatMap(_.as[FlowState]).toOption.get)(
      _.asJson.noSpaces
    )
}
