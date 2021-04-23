package app.api

import org.http4s._
import zio.RIO
import zio.interop.catz._

final class HealthEndpoint[R] extends Endpoint[R] {
  import dsl._

  val routes: HttpRoutes[RIO[R, *]] = HttpRoutes.of { case GET -> Root =>
    Ok()
  }
}
