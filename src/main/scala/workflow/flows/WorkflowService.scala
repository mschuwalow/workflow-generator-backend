package workflow.flows

import zio._

object FlowsService {

  trait Service {
    def create(request: WorkflowCreationRequest): Task[WorkflowWithId]
    def getById(id: WorkflowId): Task[WorkflowWithId]
    def delete(id: WorkflowId): Task[Option[WorkflowWithId]]
  }
}
