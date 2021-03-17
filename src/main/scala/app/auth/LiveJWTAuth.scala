package app.auth

import app.auth.{UserInfo, UserInfoService}
import tsec.authentication.JWTAuthenticator
import tsec.mac.jca.{HMACSHA256, MacSigningKey}
import zio._
import zio.interop.catz._

import scala.concurrent.duration._

private final class LiveJWTAuth(signingKey: MacSigningKey[HMACSHA256], env: LiveJWTAuth.Env) extends JWTAuth {

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

object LiveJWTAuth {
  type Env = Has[UserInfoService]

  val layer: RLayer[Env, Has[JWTAuth]] = {
    for {
      env        <- ZIO.environment[Env]
      signingKey <- HMACSHA256.generateKey[Task]
    } yield new LiveJWTAuth(signingKey, env)
  }.toLayer
}
