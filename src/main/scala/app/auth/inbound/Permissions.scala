package app.auth.inbound

import app.auth._
import zio._

trait Permissions {
  def authorize(userInfo: UserInfo, scope: Scope): Task[Unit]
}

object Permissions {

  def authorize(userInfo: UserInfo, scope: Scope): RIO[Has[Permissions], Unit] =
    ZIO.accessM(_.get.authorize(userInfo, scope))

}
