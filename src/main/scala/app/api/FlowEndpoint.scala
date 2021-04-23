package app.api

import app.auth.{Permissions, Scope, UserInfo}
import app.flows.{FlowId, FlowRepository, FlowService, unresolved}
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.Has
import zio.interop.catz._

final class FlowEndpoint[R <: FlowEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Flows)
        body     <- req.request.as[unresolved.Graph]
        result   <- FlowService.add(body)
        response <- Created(result)
      } yield response

    case GET -> Root / UUIDVar(id) asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Flows)
        flow     <- FlowRepository.getById(FlowId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FlowEndpoint {
  type Env = Has[FlowService] with Has[FlowRepository] with Has[Permissions]
}
