package app.api

import app.compiler.{ semantic, syntactic }
import app.flows.WorkflowCreationRequest
import zio.interop.catz._
import zio.logging.Logging
import zio.logging.log

final class WorkflowEndpoint[R <: Logging] extends Endpoint[R] {
  import dsl._

  def postWorkflow(workFlow: WorkflowCreationRequest) =
    for {
      _ <- log.info(s"Got workflow creation request: $workFlow")
      result = for {
        graph <- syntactic.checkCycles(workFlow.components)
        graph <- semantic.typecheck(graph)
      } yield graph
      result <- result.fold(
                 BadRequest(_),
                 t => log.info(s"Parsed graph $t") *> Ok(())
               )
    } yield result
}
