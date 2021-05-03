package app.infrastructure.http

import app.Error
import app.config.AuthConfig
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import sttp.client._
import sttp.client.circe._
import sttp.client.httpclient.zio.SttpClient
import zio._
import zio.blocking.Blocking
import zio.logging.{Logging, log}
import app.auth._
import app.auth.outbound.UserInfoService

final private class StudIpUserInfoService(
  env: StudIpUserInfoService.Env
) extends UserInfoService {
  import StudIpUserInfoService._

  def getUserInfo(username: String, password: String): Task[UserInfo] = {
    for {
      config   <- AuthConfig.get
      request   = basicRequest
                    .get(config.studipAuthUrl)
                    .auth
                    .basic(username, password)
                    .response(asJson[GetUserInfoResponse])
      response <- SttpClient.send(request).catchAll { err =>
                    log.warn(s"User info query failed with: ${err.getMessage}") *> ZIO.fail(
                      Error.AuthorizationFailed
                    )
                  }
      result   <- ZIO.fromEither(response.body).mapError(_ => Error.AuthorizationFailed)
    } yield result.toUserInto
  }.provide(env)
}

object StudIpUserInfoService {

  type Env = SttpClient with Has[AuthConfig] with Blocking with Logging

  val layer: URLayer[Env, Has[UserInfoService]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new StudIpUserInfoService(env)
  }.toLayer

  private final case class GetUserInfoResponse(
    username: String,
    user_id: String,
    perms: String
  ) {

    def toUserInto: UserInfo =
      UserInfo(user_id, username, perms)

  }

  private object GetUserInfoResponse {
    implicit val encoder: Encoder[GetUserInfoResponse] =
      deriveEncoder

    implicit val decoder: Decoder[GetUserInfoResponse] =
      deriveDecoder
  }
}
