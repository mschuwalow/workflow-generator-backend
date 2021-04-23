package app.auth

import app.Error
import app.config.AuthConfig
import zio._

final class LivePermissions(
  config: AuthConfig
) extends Permissions {

  def authorize(userInfo: UserInfo, scope: Scope) = {
    val hasPermission = scope match {
      case Scope.Admin             =>
        isAdmin(userInfo)
      case Scope.ForGroups(groups) =>
        isAdmin(userInfo) || groups.contains(userInfo.group)
      case Scope.ForUsers(ids)     =>
        isAdmin(userInfo) || ids.contains(userInfo.id)
    }
    ZIO.fail(Error.AuthorizationFailed).unless(hasPermission)
  }

  def isAdmin(userInfo: UserInfo) =
    config.adminUsers.contains(userInfo.id)

}

object LivePermissions {

  type Env = Has[AuthConfig]

  val layer: URLayer[Env, Has[Permissions]] = {
    for {
      config <- AuthConfig.get
    } yield new LivePermissions(config)
  }.toLayer

}
