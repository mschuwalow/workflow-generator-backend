package app

import app.config._
import app.flows.udf.{Python, Sys, UDFRunner}
import app.flows.{FlowRepository, FlowRunner, FlowService}
import app.forms.FormsRepository
import app.postgres.Database
import sttp.client.httpclient.zio.HttpClientZioBackend
import zio._
import zio.logging.slf4j.Slf4jLogger
import app.auth.UserInfoService
import app.api.Auth

object layers {

  val prod: ZLayer[ZEnv, Throwable, AppEnvironment] =
    ZLayer.identity[ZEnv] >+>
      AppConfig.live >+>
      Slf4jLogger.make((_, msg) => msg) >+>
      HttpClientZioBackend.layer() >+>
      UserInfoService.live >+>
      Auth.live >+>
      Database.live >+>
      FlowRepository.doobie >+>
      FormsRepository.doobie >+>
      Sys.live >+>
      Python.live >+>
      UDFRunner.live(workers = 5) >+>
      FlowRunner.stream >+>
      FlowService.live
}
