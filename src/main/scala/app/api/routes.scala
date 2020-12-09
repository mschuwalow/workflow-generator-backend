package app.api

import app.api.representations.WorkflowCreationRequest
import org.http4s._
import org.http4s.implicits._
import zio._
import zio.interop.catz._
import zio.logging.Logging
import app.WorkflowManager

object routes {

  def makeRoutes[R <: Logging with WorkflowManager]: HttpApp[RIO[R, ?]] = {
    val endpoint = new Endpoint[R] {}
    import endpoint._
    import endpoint.dsl._

    val healthEndpoint   = new HealthEndpoint[R]()
    val workflowEndpoint = new WorkflowEndpoint[R]()

    val routes = HttpRoutes.of[RTask] {
      case req @ POST -> Root / "workflows" =>
        req.as[WorkflowCreationRequest].flatMap(workflowEndpoint.postWorkflow)
      case GET -> Root / "health" =>
        healthEndpoint.healthy
    }
    ErrorHandlingMiddleware(routes)
  }.orNotFound
}
