package app

// import app.api.Auth
// import app.auth.{Permissions, UserInfoService}
// import app.config._
// import app.flows.udf._
// import app.flows.{FlowRepository, FlowRunner, FlowService}
// import app.forms.FormsRepository
// import app.postgres.Database
// import sttp.client.httpclient.zio.HttpClientZioBackend
// import zio._
// import zio.logging.slf4j.Slf4jLogger
// import app.flows.kafka.KafkaStreamsManager
// import app.postgres._
// import zio.magic._
// import app.config.{AllConfigs, configLayer}
// import app.config.HttpConfig
import app.auth.{LiveJWTAuth, LivePermissions, StudIpUserInfoService}
import app.config.ConfigLayer
import app.flows.udf.{LivePython, LiveSys, PythonUDFRunner}
import app.flows.{InMemoryFlowRunner, LiveFlowService}
import app.kafka.KafkaStreamsManager
import app.postgres.{Database, PostgresFlowRepository, PostgresFormsRepository}
import sttp.client.httpclient.zio.HttpClientZioBackend
import zio._
import zio.logging.slf4j.Slf4jLogger
import zio.magic._
import app.forms.LiveFormsService

object layers {

  val prod: ZLayer[Any, Throwable, AppEnvironment] =
    ZLayer.fromMagic[AppEnvironment](
      ConfigLayer,
      Database.migrated,
      HttpClientZioBackend.layer(),
      InMemoryFlowRunner.layer,
      KafkaStreamsManager.layer,
      LiveFlowService.layer,
      LiveFormsService.layer,
      LiveJWTAuth.layer,
      LivePermissions.layer,
      LivePython.layer,
      LiveSys.layer,
      PostgresFlowRepository.layer,
      PostgresFormsRepository.layer,
      PythonUDFRunner.layer(4),
      Slf4jLogger.makeWithAllAnnotationsAsMdc(),
      StudIpUserInfoService.layer,
      ZEnv.live
    )
}
