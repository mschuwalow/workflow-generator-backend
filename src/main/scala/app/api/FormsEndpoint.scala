package app.api

import app.auth.UserInfo
import app.forms.{Form, FormId, FormsRepository}
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.interop.catz._

final class FormsEndpoint[R <: FormsEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root asAuthed _        =>
      for {
        body     <- req.request.as[Form]
        result   <- FormsRepository.store(body)
        response <- Created(result)
      } yield response

    case GET -> Root / UUIDVar(id) asAuthed _ =>
      for {
        flow     <- FormsRepository.get(FormId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FormsEndpoint {
  type Env = FormsRepository
}
