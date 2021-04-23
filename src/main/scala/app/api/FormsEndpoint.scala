package app.api

import app.auth.{Permissions, Scope, UserInfo}
import app.forms.{CreateFormRequest, FormId, FormsRepository, FormsService}
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
        flow     <- FormsRepository.get(FormId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FormsEndpoint {
  type Env = Has[FormsRepository] with Has[Permissions] with Has[FormsService]
}
