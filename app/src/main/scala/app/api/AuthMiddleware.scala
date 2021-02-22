package app.api

import cats.Monad
import cats.data.Kleisli
import org.http4s.{HttpRoutes, Request, Response, Status}
import tsec.authentication.{Authenticator, TSecMiddleware}

object AuthMiddleware {

  def apply[F[_]: Monad, I, V, A](authenticator: Authenticator[F, I, V, A])(
    k: SecuredRoutes[F, V, A]
  ): HttpRoutes[F] = {
    val middleware = TSecMiddleware(
      Kleisli(authenticator.extractAndValidate),
      (_: Request[F]) => Monad[F].point(Response[F](Status.Unauthorized))
    )
    middleware(k)
  }
}
