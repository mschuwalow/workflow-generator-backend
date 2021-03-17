package app.auth

import app.auth.UserInfo
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import zio._

trait JWTAuth {
  def auth(username: String, password: String): Task[AugmentedJWT[HMACSHA256, UserInfo]]
  def getTSecAuthenticator[R]: JWTAuthenticator[RIO[R, *], UserInfo, UserInfo, HMACSHA256]
}

object JWTAuth {

  def auth(username: String, password: String): RIO[Has[JWTAuth], AugmentedJWT[HMACSHA256, UserInfo]] =
    ZIO.accessM(_.get.auth(username, password))

  def getTSecAuthenticator[R]: URIO[Has[JWTAuth], JWTAuthenticator[RIO[R, *], UserInfo, UserInfo, HMACSHA256]] =
    ZIO.access(_.get.getTSecAuthenticator[R])

}
