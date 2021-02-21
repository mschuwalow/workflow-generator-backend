package app.auth

import app.config.AuthConfig
import zio._
import sttp.client.httpclient.zio.SttpClient
import sttp.client._

object UserInfoService {

  trait Service {
    def getUserInfo(username: String, password: String): UIO[UserInfo]
  }

  val live: ZLayer[SttpClient with AuthConfig, Nothing, UserInfoService] = ZLayer.fromFunction { env =>
    new Service {
      def getUserInfo(username: String, password: String): UIO[UserInfo] = {
        for {
          _ <- AuthConfig.get
          _ = basicRequest
        } yield ???
      }.provide(env)
    }
  }

  def getUserInfo(username: String, password: String): URIO[UserInfoService, UserInfo] =
    ZIO.accessM(_.get.getUserInfo(username, password))

}
