package app.api

import cats.data.Kleisli
import zio._
import app.Error
import zio.interop.catz._
import org.http4s.Response
import org.http4s.Request
import cats.data.OptionT
import org.http4s.dsl.Http4sDsl

object ErrorHandlingMiddleware {

  def apply[R](
    k: Kleisli[OptionT[RIO[R, *], *], Request[RIO[R, *]], Response[RIO[R, *]]]
  ): Kleisli[OptionT[RIO[R, *], *], Request[RIO[R, *]], Response[RIO[R, *]]] = {
    val dsl = Http4sDsl.apply[RIO[R, *]]
    import dsl._
    Kleisli { req =>
      OptionT {
        k.run(req).value.catchSome {
          case Error.ValidationFailed(msg) => BadRequest(msg).map(Some(_))
          case Error.NotFound              => NotFound().map(Some(_))
        }
      }
    }
  }
}
