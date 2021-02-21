package app.api

import app.auth.UserInfo
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.{HttpRoutes, Response, Status}
import tsec.authentication._
import tsec.mac.jca.HMACSHA256
import zio.RIO
import zio.interop.catz._

final class AuthEndpoint[R <: AuthEndpoint.Env] extends Endpoint[R] {
  import AuthEndpoint._
  import dsl._

  val authedRoutes = TSecAuthService[UserInfo, AugmentedJWT[HMACSHA256, UserInfo], RTask] {
    case req @ POST -> Root / "refresh" asAuthed _ =>
      for {
        authenticator <- Auth.getTSecAuthenticator[R]
        newToken      <- authenticator.refresh(req.authenticator)
        response       = authenticator.embed(Response(Status.Ok), newToken)
      } yield response
  }

  val routes: HttpRoutes[RIO[R, *]] = HttpRoutes.of {
    case req @ POST -> Root / "login" =>
      for {
        body          <- req.as[LoginRequest]
        token         <- Auth.auth(body.username, body.password)
        authenticator <- Auth.getTSecAuthenticator[R]
        response       = authenticator.embed(Response(Status.Ok), token)
      } yield response
  }
}

object AuthEndpoint {
  type Env = Auth

  final case class LoginRequest(
    username: String,
    password: String
  )

  object LoginRequest {
    implicit val decoder: Decoder[LoginRequest] =
      deriveDecoder
  }
}
