package app.api

import cats.syntax.semigroupk._
import org.http4s._
import org.http4s.implicits._
import zio._
import zio.interop.catz._

object Router {

  type Env = FlowEndpoint.Env with GeneratedFormsEndpoint.Env with FormsEndpoint.Env

  def makeApp[R <: Env]: URIO[R, HttpApp[RIO[R, ?]]] = {
    val healthEndpoint         = new HealthEndpoint[R]()
    val flowEndpoint           = new FlowEndpoint[R]()
    val formsEndpoint          = new FormsEndpoint[R]()
    val generatedFormsEndpoint = new GeneratedFormsEndpoint[R]()

    for {
      generatedFormsRoutes <- generatedFormsEndpoint.routes
      healthRoutes          = healthEndpoint.routes
      flowRoutes            = flowEndpoint.routes
      formsRoutes           = formsEndpoint.routes
      routes                = healthRoutes <+> flowRoutes <+> formsRoutes <+> generatedFormsRoutes
    } yield ErrorHandlingMiddleware(routes).orNotFound
  }
}
