package app

import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import zio.RIO
import zio.interop.catz._

import scala.util.control.NoStackTrace

sealed abstract class Error extends NoStackTrace {
  def httpResponse[R](dsl: Http4sDsl[RIO[R, *]]): RIO[R, Response[RIO[R, *]]]
}

object Error {

  final case class GraphValidationFailed(reason: String) extends Error {
    def httpResponse[R](dsl: Http4sDsl[RIO[R, *]]) = {
      import dsl._
      dsl.BadRequest(s"checking the graph failed: $reason")
    }
  }

  case object NotFound extends Error {
    def httpResponse[R](dsl: Http4sDsl[RIO[R, *]]) = {
      import dsl._
      dsl.NotFound()
    }
  }

}
