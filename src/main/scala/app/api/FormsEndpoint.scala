package app.api

import app.forms.{Form, FormId, FormsRepository}
import zio.interop.catz._
import tsec.authentication._

final class FormsEndpoint[R <: FormsEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes =
    Auth.getTSecAuthenticator[R].map { auth =>
      SecuredRequestHandler(auth).liftService {
        TSecAuthService {
          case req @ POST -> Root / "forms" asAuthed _ =>
            for {
              body     <- req.request.as[Form]
              result   <- FormsRepository.store(body)
              response <- Created(result)
            } yield response

          case GET -> Root / "forms" / UUIDVar(id) asAuthed _ =>
            for {
              flow     <- FormsRepository.get(FormId(id))
              response <- Ok(flow)
            } yield response
        }
      }
    }
}

object FormsEndpoint {
  type Env = FormsRepository with Auth
}
