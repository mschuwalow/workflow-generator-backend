package app.auth

import app.Error
import app.config.AuthConfig
import zio._

object Permissions {

  trait Service {
    def authorize(userInfo: UserInfo, scope: Scope): Task[Unit]
  }

  val live: ZLayer[AuthConfig, Nothing, Permissions] = ZLayer.fromService { config =>
    def isAdmin(userInfo: UserInfo) =
      config.adminUsers.contains(userInfo.id)

    new Service {
      def authorize(userInfo: UserInfo, scope: Scope) = {
        val hasPermission = scope match {
          case Scope.Admin             =>
            isAdmin(userInfo)
          case Scope.Flows             =>
            isAdmin(userInfo)
          case Scope.Forms             =>
            isAdmin(userInfo)
          case Scope.ForGroups(groups) =>
            isAdmin(userInfo) || groups.contains(userInfo.group)
          case Scope.ForUsers(ids)     =>
            isAdmin(userInfo) || ids.contains(userInfo.id)
        }
        ZIO.fail(Error.AuthorizationFailed).unless(hasPermission)
      }
    }
  }

  def authorize(userInfo: UserInfo, scope: Scope): RIO[Permissions, Unit] =
    ZIO.accessM(_.get.authorize(userInfo, scope))

}
