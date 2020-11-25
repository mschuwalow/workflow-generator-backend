package app.api
import app.flows.WorkflowCreationRequest
import zio.interop.catz._
import zio.logging.Logging
import zio.logging.log

final class WorkflowEndpoint[R <: Logging] extends Endpoint[R] {
  import dsl._

  def postWorkflow(workFlow: WorkflowCreationRequest) =
    log.info(s"Got workflow creation request: $workFlow") *> Ok()
}
