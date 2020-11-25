package app.api

import app.flows.WorkflowCreationRequest
import org.http4s._
import org.http4s.implicits._
import zio._
import zio.interop.catz._
import zio.logging.Logging

object routes {

  def makeRoutes[R <: Logging]: HttpApp[RIO[R, ?]] = {
    val endpoint = new Endpoint[R] {}
    import endpoint._
    import endpoint.dsl._

    val healthEndpoint   = new HealthEndpoint[R]()
    val workflowEndpoint = new WorkflowEndpoint[R]()

    HttpRoutes.of[RTask] {
      case req @ POST -> Root / "workflows" =>
        req.as[WorkflowCreationRequest].flatMap(workflowEndpoint.postWorkflow)
      case GET -> Root / "health" =>
        healthEndpoint.healthy
    }
  }.orNotFound
}
