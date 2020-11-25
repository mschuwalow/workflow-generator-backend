package app.api

import org.http4s._
import zio._
import zio.interop.catz._

final class HealthEndpoint[R] extends Endpoint[R] {
  import dsl._

  val healthy: RTask[Response[RIO[R, ?]]] =
    Ok()
}
