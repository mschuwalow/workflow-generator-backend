package workflow.api

import io.circe.{ Decoder, Encoder }
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import zio._
import zio.interop.catz._

abstract class Endpoint[R] {
  type RTask[A] = RIO[R, A]

  val dsl: Http4sDsl[RTask] = Http4sDsl[RTask]

  implicit def circeJsonDecoder[A: Decoder]: EntityDecoder[RTask, A] =
    jsonOf[RTask, A]

  implicit def circeJsonEncoder[A: Encoder]: EntityEncoder[RTask, A] =
    jsonEncoderOf[RTask, A]
}
