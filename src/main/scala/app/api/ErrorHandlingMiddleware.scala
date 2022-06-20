package app.api

import app.Error
import cats.data.{Kleisli, OptionT}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio._
import zio.logging.{Logging, log}

object ErrorHandlingMiddleware {
  type Env = Logging

  def apply[R <: Env](
    k: HttpRoutes[RIO[R, *]]
  ): HttpRoutes[RIO[R, *]] = {
    val dsl = Http4sDsl.apply[RIO[R, *]]
    Kleisli { req =>
      OptionT {
        k.run(req).value.absorb.catchSome { case err: Error =>
          log.warn(s"Request failed: $err") *>
            err.httpResponse[R](dsl).map(Some(_))
        }
      }
    }
  }
}
