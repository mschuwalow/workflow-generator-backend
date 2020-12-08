package app.api.representations

import app.domain.raw
import io.circe._
import io.circe.generic.semiauto._

final case class WorkflowCreationRequest(components: raw.Graph)

object WorkflowCreationRequest {

  implicit val decoder: Decoder[WorkflowCreationRequest] =
    deriveDecoder
}
