package app.api

import app.flows.{FlowId, FlowRepository, FlowService, unresolved}
import org.http4s.HttpRoutes
import zio.RIO
import zio.interop.catz._
import zio.logging.Logging

final class FlowEndpoint[R <: FlowEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val routes: HttpRoutes[RIO[R, *]] = HttpRoutes.of {
    case req @ POST -> Root / "workflows"        =>
      for {
        body     <- req.as[unresolved.Graph]
        result   <- FlowService.add(body)
        response <- Created(result)
      } yield response

    case GET -> Root / "workflows" / UUIDVar(id) =>
      for {
        flow     <- FlowRepository.getById(FlowId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FlowEndpoint {
  type Env = Logging with FlowService with FlowRepository
}
