package app.api

import app.Error
import cats.data.{Kleisli, OptionT}
import org.http4s.dsl.Http4sDsl
import org.http4s.{Request, Response}
import zio._

object ErrorHandlingMiddleware {

  def apply[R](
    k: Kleisli[OptionT[RIO[R, *], *], Request[RIO[R, *]], Response[RIO[R, *]]]
  ): Kleisli[OptionT[RIO[R, *], *], Request[RIO[R, *]], Response[RIO[R, *]]] = {
    val dsl = Http4sDsl.apply[RIO[R, *]]
    Kleisli { req =>
      OptionT {
        k.run(req).value.catchSome {
          case err: Error => err.httpResponse[R](dsl).map(Some(_))
        }
      }
    }
  }
}
