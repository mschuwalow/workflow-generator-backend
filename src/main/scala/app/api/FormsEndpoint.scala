package app.api

import app.auth.inbound.Permissions
import app.auth.{Scope, UserInfo}
import app.forms.inbound.FormsService
import app.forms.{CreateFormRequest, FormId}
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.Has
import zio.interop.catz._

final class FormsEndpoint[R <: FormsEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Admin)
        body     <- req.request.as[CreateFormRequest]
        result   <- FormsService.create(body)
        response <- Created(result)
      } yield response

    case GET -> Root / UUIDVar(id) asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Admin)
        flow     <- FormsService.getById(FormId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FormsEndpoint {
  type Env = Has[Permissions] with Has[FormsService]
}
