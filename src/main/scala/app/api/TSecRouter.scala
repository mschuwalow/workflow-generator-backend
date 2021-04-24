package app.api

import cats._
import cats.data.{Kleisli, OptionT}
import cats.syntax.semigroupk._
import org.http4s._
import tsec.authentication.SecuredRequest

object TSecRouter {

  def apply[F[_]: Monad, V, A](mappings: (String, SecuredRoutes[F, V, A])*): SecuredRoutes[F, V, A] =
    define(mappings: _*)(Kleisli.liftF(OptionT.none))

  def define[F[_]: Monad, V, A](
    mappings: (String, Kleisli[OptionT[F, *], SecuredRequest[F, V, A], Response[F]])*
  )(default: SecuredRoutes[F, V, A]): SecuredRoutes[F, V, A] =
    mappings.sortBy(_._1.length).foldLeft(default) { case (acc, (prefix, routes)) =>
      val prefixSegments = toSegments(prefix)
      if (prefixSegments.isEmpty) routes <+> acc
      else
        Kleisli { req =>
          (
            if (toSegments(req.request.pathInfo).startsWith(prefixSegments))
              routes.local(translate(prefix)) <+> acc
            else
              acc
          )(req)
        }
    }

  private def translate[F[_], V, A](prefix: String)(req: SecuredRequest[F, V, A]): SecuredRequest[F, V, A] = {
    val newCaret = prefix match {
      case "/"                    => 0
      case x if x.startsWith("/") => x.length
      case x                      => x.length + 1
    }

    val oldCaret = req.request.attributes
      .lookup(Request.Keys.PathInfoCaret)
      .getOrElse(0)
    req.copy(request = req.request.withAttribute(Request.Keys.PathInfoCaret, oldCaret + newCaret))
  }

  private def toSegments(path: String): List[String] =
    path.split("/").filterNot(_.trim.isEmpty).toList
}
