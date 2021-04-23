package app.api

import org.http4s.HttpRoutes
import zio._
import org.http4s.StaticFile
import zio.blocking.Blocking
import zio.interop.catz._
import cats.effect.Blocker

final class SwaggerEndpoint[R <: SwaggerEndpoint.Env] extends Endpoint[R] {
  import dsl._

  val routes: HttpRoutes[RIO[R, *]] = HttpRoutes.of {
    case req @ GET -> Root / path if SwaggerEndpoint.fileNames.contains(path) =>
      ZIO.service[Blocking.Service].flatMap { blocking =>
        val blocker = Blocker.liftExecutionContext(blocking.blockingExecutor.asEC)
        StaticFile
          .fromResource("/api.yaml", blocker, Some(req))
          .getOrElseF(NotFound())
      }
  }
}

object SwaggerEndpoint {

  type Env = Blocking

  final val fileNames = Set("swagger.yaml", "swagger.yml")

}
