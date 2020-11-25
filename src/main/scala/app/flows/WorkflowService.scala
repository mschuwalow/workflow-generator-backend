package app.flows

import zio._

object FlowsService {

  trait Service {
    def create(request: WorkflowCreationRequest): Task[Workflow]
    def getById(id: WorkflowId): Task[Workflow]
    def delete(id: WorkflowId): Task[Option[Workflow]]
  }
}
