package app.api

import app.auth.{Scope, UserInfo}
import app.auth.inbound.Permissions
import app.flows.{FlowId, unresolved}
import app.flows.inbound.FlowService
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.Has
import zio.interop.catz._

final class FlowEndpoint[R <: FlowEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Admin)
        body     <- req.request.as[unresolved.CreateFlowRequest]
        result   <- FlowService.add(body)
        response <- Created(result)
      } yield response

    case GET -> Root / UUIDVar(id) asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Admin)
        flow     <- FlowService.getById(FlowId(id))
        response <- Ok(flow)
      } yield response

    case DELETE -> Root / UUIDVar(id) asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Admin)
        _        <- FlowService.delete(FlowId(id))
        response <- NoContent()
      } yield response
  }
}

object FlowEndpoint {
  type Env = Has[FlowService] with Has[Permissions]
}
