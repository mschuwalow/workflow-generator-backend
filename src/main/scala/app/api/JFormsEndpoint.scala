package app.api

import app.auth.inbound.Permissions
import app.auth.{Scope, UserInfo}
import app.jforms._
import app.jforms.inbound.JFormsService
import org.http4s.Request
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.interop.catz._
import zio.{Has, ZIO}

final class JFormsEndpoint[R <: JFormsEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root asAuthed user =>
      for {
        _        <- Permissions.authorize(user, Scope.Admin)
        body     <- req.request.as[CreateJFormRequest]
        result   <- JFormsService.create(body)
        response <- Created(result)
      } yield response

    case GET -> Root / UUIDVar(id) asAuthed user =>
      for {
        form     <- JFormsService.getById(JFormId(id))
        _        <- ZIO.foreach_(form.perms)(Permissions.authorize(user, _))
        response <- Ok(form)
      } yield response

    case req @ POST -> Root / UUIDVar(id) / "submissions" asAuthed user =>
      for {
        form     <- JFormsService.getById(JFormId(id))
        _        <- ZIO.foreach_(form.perms)(Permissions.authorize(user, _))
        data     <- decodeSubmission(form, req.request)
        _        <- JFormsService.publish(form)(data)
        response <- Accepted()
      } yield response
  }

  private def decodeSubmission(form: JForm, request: Request[RTask]): RTask[form.outputType.Scala] = {
    implicit val decoder = form.outputType.deriveDecoder
    request.as[form.outputType.Scala]
  }
}

object JFormsEndpoint {
  type Env = Has[Permissions] with Has[JFormsService]
}
