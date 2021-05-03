package app.auth.outbound

import zio._
import app.auth.UserInfo

trait UserInfoService {
  def getUserInfo(username: String, password: String): Task[UserInfo]
}

object UserInfoService {

  def getUserInfo(username: String, password: String): RIO[Has[UserInfoService], UserInfo] =
    ZIO.accessM(_.get.getUserInfo(username, password))

}
