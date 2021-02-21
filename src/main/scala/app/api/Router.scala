package app.api

import cats.syntax.semigroupk._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.{Router => Http4sRouter}
import zio._
import zio.interop.catz._

object Router {

  type Env = FlowEndpoint.Env with GeneratedFormsEndpoint.Env with FormsEndpoint.Env with Auth

  def makeApp[R <: Env]: URIO[R, HttpApp[RIO[R, *]]] = {
    val healthEndpoint         = new HealthEndpoint[R]()
    val flowEndpoint           = new FlowEndpoint[R]()
    val formsEndpoint          = new FormsEndpoint[R]()
    val generatedFormsEndpoint = new GeneratedFormsEndpoint[R]("/generated")
    val authEndpoint           = new AuthEndpoint[R]()

    val normalRoutes = Http4sRouter(
      "/health" -> healthEndpoint.routes,
      "/auth"   -> authEndpoint.routes
    )

    val securedRoutes = for {
      authenticator        <- Auth.getTSecAuthenticator[R]
      authRoutes            = authEndpoint.authedRoutes
      flowRoutes            = flowEndpoint.authedRoutes
      formsRoutes           = formsEndpoint.authedRoutes
      generatedFormsRoutes <- generatedFormsEndpoint.authedRoutes
    } yield AuthMiddleware(authenticator)(
      TSecRouter(
        "/auth"      -> authRoutes,
        "/workflows" -> flowRoutes,
        "/forms"     -> formsRoutes,
        "/generated" -> generatedFormsRoutes
      )
    )

    for {
      secured <- securedRoutes
      normal   = normalRoutes
    } yield ErrorHandlingMiddleware(normal <+> secured).orNotFound
  }
}
