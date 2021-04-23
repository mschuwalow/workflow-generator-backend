package app

import org.http4s.dsl.Http4sDsl
import org.http4s.{Response, Status}
import zio.interop.catz._
import zio.{RIO, ZIO}

import scala.util.control.NoStackTrace

sealed abstract class Error extends NoStackTrace {
  def httpResponse[R](dsl: Http4sDsl[RIO[R, *]]): RIO[R, Response[RIO[R, *]]]
}

object Error {
  final case class GraphValidationFailed(reason: String) extends Error {
    def httpResponse[R](dsl: Http4sDsl[RIO[R, *]]) = {
      dsl.BadRequest(s"checking the graph failed: $reason")
    }
  }

  case object AuthorizationFailed extends Error {
    def httpResponse[R](dsl: Http4sDsl[RIO[R, *]]) =
      ZIO.succeed(
        Response(
          status = Status.Unauthorized
        )
      )
  }

  case object NotFound extends Error {
    def httpResponse[R](dsl: Http4sDsl[RIO[R, *]]) = {
      dsl.NotFound()
    }
  }

}
