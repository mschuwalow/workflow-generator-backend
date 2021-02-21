package app.api

import app.auth.UserInfo
import app.flows.{FlowId, FlowRepository, FlowService, unresolved}
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.interop.catz._

final class FlowEndpoint[R <: FlowEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root / "workflows" asAuthed _        =>
      for {
        body     <- req.request.as[unresolved.Graph]
        result   <- FlowService.add(body)
        response <- Created(result)
      } yield response

    case GET -> Root / "workflows" / UUIDVar(id) asAuthed _ =>
      for {
        flow     <- FlowRepository.getById(FlowId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FlowEndpoint {
  type Env = FlowService with FlowRepository
}
