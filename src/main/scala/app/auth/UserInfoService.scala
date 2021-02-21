package app.auth

import app.config.AuthConfig
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import sttp.client._
import sttp.client.circe._
import sttp.client.httpclient.zio.SttpClient
import zio._
import zio.blocking.Blocking
import zio.logging.{Logging, log}

object UserInfoService {

  trait Service {
    def getUserInfo(username: String, password: String): Task[UserInfo]
  }

  val live: ZLayer[SttpClient with AuthConfig with Blocking with Logging, Nothing, UserInfoService] =
    ZLayer.fromFunction { env =>
      new Service {
        def getUserInfo(username: String, password: String): Task[UserInfo] = {
          for {
            config   <- AuthConfig.get
            request   = basicRequest
                          .get(config.studipAuthUrl)
                          .auth
                          .basic(username, password)
                          .response(asJson[internal.GetUserInfoResponse])
            response <- SttpClient.send(request).catchAll { err =>
                          log.warn(s"User info query failed with: ${err.getMessage}") *> ZIO.fail(
                            app.Error.AuthorizationFailed
                          )
                        }
            result   <- response.body.fold(ZIO.die(_), ZIO.succeed(_))
          } yield result.toUserInto
        }.provide(env)
      }
    }

  private object internal {

    final case class GetUserInfoResponse(
      username: String,
      user_id: String,
      perms: String
    ) {

      def toUserInto: UserInfo =
        UserInfo(username, perms)

    }

    object GetUserInfoResponse {
      implicit val encoder: Encoder[GetUserInfoResponse] =
        deriveEncoder

      implicit val decoder: Decoder[GetUserInfoResponse] =
        deriveDecoder
    }

  }

  def getUserInfo(username: String, password: String): RIO[UserInfoService, UserInfo] =
    ZIO.accessM(_.get.getUserInfo(username, password))

}
