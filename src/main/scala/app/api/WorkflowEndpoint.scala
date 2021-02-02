package app.api

import app.WorkflowManager
import app.api.representations.WorkflowCreationRequest
import app.domain.FlowId
import app.repository.WorkflowRepository
import zio.interop.catz._
import zio.logging.Logging

final class WorkflowEndpoint[R <: Logging with WorkflowManager with WorkflowRepository]
    extends Endpoint[R] {
  import dsl._

  def postWorkflow(workFlow: WorkflowCreationRequest) =
    for {
      result   <- WorkflowManager.add(workFlow.components)
      response <- Ok(result)
    } yield response

  def getWorkflow(workflowId: FlowId) =
    for {
      flow     <- WorkflowRepository.getById(workflowId)
      response <- Ok(flow)
    } yield response
}
