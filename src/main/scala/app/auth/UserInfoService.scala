package app.auth

import zio._

trait UserInfoService {
  def getUserInfo(username: String, password: String): Task[UserInfo]
}

object UserInfoService {

  def getUserInfo(username: String, password: String): RIO[Has[UserInfoService], UserInfo] =
    ZIO.accessM(_.get.getUserInfo(username, password))

}
