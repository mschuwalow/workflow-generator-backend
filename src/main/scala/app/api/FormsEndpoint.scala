package app.api

import app.forms.{Form, FormId, FormsRepository}
import org.http4s.HttpRoutes
import zio.RIO
import zio.interop.catz._

final class FormsEndpoint[R <: FormsEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val routes: HttpRoutes[RIO[R, *]] = HttpRoutes.of {
    case req @ POST -> Root / "forms"        =>
      for {
        body     <- req.as[Form]
        result   <- FormsRepository.store(body)
        response <- Created(result)
      } yield response

    case GET -> Root / "forms" / UUIDVar(id) =>
      for {
        flow     <- FormsRepository.get(FormId(id))
        response <- Ok(flow)
      } yield response
  }
}

object FormsEndpoint {
  type Env = FormsRepository
}
