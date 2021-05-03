package app.api

import app.auth.inbound.JWTAuth
import cats.syntax.semigroupk._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.{Router => Http4sRouter}
import zio._
import zio.interop.catz._

object Router {

  type Env = FlowEndpoint.Env
    with RenderedFormsEndpoint.Env
    with FormsEndpoint.Env
    with SwaggerEndpoint.Env
    with Has[JWTAuth]

  def makeApp[R <: Env]: URIO[R, HttpApp[RIO[R, *]]] = {
    val healthEndpoint        = new HealthEndpoint[R]()
    val flowEndpoint          = new FlowEndpoint[R]()
    val formsEndpoint         = new FormsEndpoint[R]()
    val renderedFormsEndpoint = new RenderedFormsEndpoint[R]("/rendered")
    val authEndpoint          = new AuthEndpoint[R]()
    val swaggerEndpoint       = new SwaggerEndpoint[R]()

    val normalRoutes = Http4sRouter(
      "/health" -> healthEndpoint.routes,
      "/auth"   -> authEndpoint.routes
    )

    val makeSecuredRoutes = for {
      authenticator       <- JWTAuth.getTSecAuthenticator[R]
      flowRoutes           = flowEndpoint.authedRoutes
      formsRoutes          = formsEndpoint.authedRoutes
      renderedFormsRoutes <- renderedFormsEndpoint.authedRoutes
    } yield AuthMiddleware(authenticator)(
      TSecRouter(
        "/flows"    -> flowRoutes,
        "/forms"    -> formsRoutes,
        "/rendered" -> renderedFormsRoutes
      )
    )

    for {
      securedRoutes <- makeSecuredRoutes
      allRoutes      = swaggerEndpoint.routes <+> normalRoutes <+> securedRoutes
    } yield ErrorHandlingMiddleware(allRoutes).orNotFound
  }
}
