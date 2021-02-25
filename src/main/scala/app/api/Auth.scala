package app.api

import app.auth.{UserInfo, UserInfoService}
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import zio._
import zio.interop.catz._

import scala.concurrent.duration._

object Auth {

  trait Service {
    def auth(username: String, password: String): Task[AugmentedJWT[HMACSHA256, UserInfo]]
    def getTSecAuthenticator[R]: JWTAuthenticator[RIO[R, *], UserInfo, UserInfo, HMACSHA256]
  }

  val live: ZLayer[UserInfoService, Throwable, Auth] = ZLayer.fromFunctionM { env =>
    for {
      signingKey <- HMACSHA256.generateKey[Task]
    } yield new Service {
      def auth(username: String, password: String) = {
        for {
          userInfo     <- UserInfoService.getUserInfo(username, password)
          authenticator = getTSecAuthenticator[Any]
          token        <- authenticator.create(userInfo)
        } yield token
      }.provide(env)

      def getTSecAuthenticator[R] =
        JWTAuthenticator.pstateless.inBearerToken[RIO[R, *], UserInfo, HMACSHA256](
          20.minutes,
          maxIdle = None,
          signingKey
        )
    }
  }

  def auth(username: String, password: String): RIO[Auth, AugmentedJWT[HMACSHA256, UserInfo]] =
    ZIO.accessM(_.get.auth(username, password))

  def getTSecAuthenticator[R]: URIO[Auth, JWTAuthenticator[RIO[R, *], UserInfo, UserInfo, HMACSHA256]] =
    ZIO.access(_.get.getTSecAuthenticator[R])

}
