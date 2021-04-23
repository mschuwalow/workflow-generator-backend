package app.api

import app.Error
import cats.data.{Kleisli, OptionT}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio._

object ErrorHandlingMiddleware {

  def apply[R](
    k: HttpRoutes[RIO[R, *]]
  ): HttpRoutes[RIO[R, *]] = {
    val dsl = Http4sDsl.apply[RIO[R, *]]
    Kleisli { req =>
      OptionT {
        k.run(req).value.catchSome { case err: Error =>
          err.httpResponse[R](dsl).map(Some(_))
        }
      }
    }
  }
}
