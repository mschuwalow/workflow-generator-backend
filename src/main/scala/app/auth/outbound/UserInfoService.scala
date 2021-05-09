package app.auth.outbound

import app.auth.UserInfo
import zio._

trait UserInfoService {
  def getUserInfo(username: String, password: String): Task[UserInfo]
}

object UserInfoService {

  def getUserInfo(username: String, password: String): RIO[Has[UserInfoService], UserInfo] =
    ZIO.accessM(_.get.getUserInfo(username, password))

}
