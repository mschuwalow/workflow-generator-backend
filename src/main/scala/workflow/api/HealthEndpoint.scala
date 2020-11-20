package workflow.api

import zio._
import zio.interop.catz._
import org.http4s._

final class HealthEndpoint[R] extends Endpoint[R] {
  import dsl._

  val healthy: RTask[Response[RIO[R, ?]]] =
    Ok()
}
