package app.api

import app.auth.{Permissions, Scope, UserInfo}
import app.forms.{Form, FormId, FormsRepository}
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.interop.catz._

final class FormsEndpoint[R <: FormsEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root asAuthed user        =>
      for {
        _        <- Permissions.authorize(user, Scope.Forms)
        body     <- req.request.as[Form]
        result   <- FormsRepository.store(body)
        response <- Created(result)
      } yield response

    case GET -> Root / UUIDVar(id) asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Forms)
        flow     <- FormsRepository.get(FormId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FormsEndpoint {
  type Env = FormsRepository with Permissions
}
