package app

import cats.data.{Kleisli, OptionT}
import org.http4s.Response
import tsec.authentication.SecuredRequest

package object api {

  type SecuredRoutes[F[_], V, A] = Kleisli[OptionT[F, *], SecuredRequest[F, V, A], Response[F]]

}
