package app.api

import cats.syntax.semigroupk._
import org.http4s._
import org.http4s.implicits._
import zio._
import zio.interop.catz._

object Router {

  type Env = FlowEndpoint.Env with GeneratedFormsEndpoint.Env with FormsEndpoint.Env

  def makeApp[R <: Env]: URIO[R, HttpApp[RIO[R, *]]] = {
    val healthEndpoint         = new HealthEndpoint[R]()
    val flowEndpoint           = new FlowEndpoint[R]()
    val formsEndpoint          = new FormsEndpoint[R]()
    val generatedFormsEndpoint = new GeneratedFormsEndpoint[R]()
    val authEndpoint           = new AuthEndpoint[R]()

    for {
      generatedFormsRoutes <- generatedFormsEndpoint.routes
      authRoutes            = authEndpoint.routes
      healthRoutes          = healthEndpoint.routes
      flowRoutes           <- flowEndpoint.authedRoutes
      formsRoutes          <- formsEndpoint.authedRoutes
      routes                = healthRoutes <+> authRoutes <+> flowRoutes <+> formsRoutes <+> generatedFormsRoutes
    } yield ErrorHandlingMiddleware(routes).orNotFound
  }
}
