package workflow

package object api {
  import org.http4s._
  import org.http4s.implicits._
  import zio._
  import zio.interop.catz._

  def makeRoutes[R]: HttpApp[RIO[R, ?]] = {
    val endpoint = new Endpoint[R] {}
    import endpoint._
    import endpoint.dsl._

    val healthEndpoint = new HealthEndpoint[R]()

    HttpRoutes.of[RTask] {
      case GET -> Root / "health" =>
        healthEndpoint.healthy
    }
  }.orNotFound
}
