package app.api

import cats.Monad
import cats.data.{Kleisli, OptionT}
import org.http4s.{Request, Response, Status}
import tsec.authentication.{Authenticator, SecuredRequest, TSecMiddleware}

object AuthMiddleware {

  def apply[F[_]: Monad, I, V, A](authenticator: Authenticator[F, I, V, A])(
    k: Kleisli[OptionT[F, *], SecuredRequest[F, V, A], Response[F]]
  ): Kleisli[OptionT[F, *], Request[F], Response[F]] = {
    val middleware = TSecMiddleware(
      Kleisli(authenticator.extractAndValidate),
      (_: Request[F]) => Monad[F].point(Response[F](Status.Unauthorized))
    )
    middleware(k)
  }
}
