package app.api

import app.api.representations.WorkflowCreationRequest
import zio.interop.catz._
import zio.logging.Logging
import app.WorkflowManager

final class WorkflowEndpoint[R <: Logging with WorkflowManager] extends Endpoint[R] {
  import dsl._

  def postWorkflow(workFlow: WorkflowCreationRequest) =
    for {
      result   <- WorkflowManager.add(workFlow.components)
      response <- Ok(result)
    } yield response
}
