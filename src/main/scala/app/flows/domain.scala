package app.flows

import io.circe._
import io.circe.generic.semiauto._
import app.backend.nodes.ComponentId
import app.backend.nodes.raw._

final case class WorkflowId(value: String) extends AnyVal

object WorkflowId {
  implicit val encoder: Encoder[WorkflowId] = Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[WorkflowId] =
    Decoder[String].map(WorkflowId.apply)
}

final case class WorkflowCreationRequest(
  components: Map[ComponentId, Component])

object WorkflowCreationRequest {

  implicit val decoder: Decoder[WorkflowCreationRequest] =
    deriveDecoder
}

final case class Workflow()

object Workflow {

  implicit val decoder: Decoder[Workflow] =
    deriveDecoder
}
