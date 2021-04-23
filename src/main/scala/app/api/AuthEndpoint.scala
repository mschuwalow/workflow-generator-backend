package app.api

import app.auth.JWTAuth
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.{HttpRoutes, Response, Status}
import zio.interop.catz._
import zio.{Has, RIO}

final class AuthEndpoint[R <: AuthEndpoint.Env] extends Endpoint[R] {
  import AuthEndpoint._
  import dsl._

  val routes: HttpRoutes[RIO[R, *]] = HttpRoutes.of { case req @ POST -> Root / "login" =>
    for {
      body          <- req.as[LoginRequest]
      token         <- JWTAuth.auth(body.username, body.password)
      authenticator <- JWTAuth.getTSecAuthenticator[R]
      response       = authenticator.embed(Response(Status.Ok), token)
    } yield response
  }
}

object AuthEndpoint {
  type Env = Has[JWTAuth]

  final case class LoginRequest(
    username: String,
    password: String
  )

  object LoginRequest {
    implicit val decoder: Decoder[LoginRequest] =
      deriveDecoder
  }
}
