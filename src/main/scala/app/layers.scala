package app

import app.auth.{LiveJWTAuth, LivePermissions, StudIpUserInfoService}
import app.config.ConfigLayer
import app.flows.udf.{LivePython, LiveSys, PythonUDFRunner}
import app.flows.{LiveFlowRunner, LiveFlowService}
import app.forms.LiveFormsService
import app.kafka.KafkaStreamsManager
import app.postgres.{Database, PostgresFlowOffsetRepository, PostgresFlowRepository, PostgresFormsRepository}
import sttp.client.httpclient.zio.HttpClientZioBackend
import zio._
import zio.logging.slf4j.Slf4jLogger

object layers {

  val prod: ZLayer[Any, Throwable, AppEnvironment] =
    ZEnv.live >+>
    ConfigLayer >+>
    Slf4jLogger.makeWithAllAnnotationsAsMdc() >+>
    Database.migrated >+>
    PostgresFlowOffsetRepository.layer >+>
    PostgresFlowRepository.layer >+>
    PostgresFormsRepository.layer >+>
    KafkaStreamsManager.layer >+>
    HttpClientZioBackend.layer() >+>
    StudIpUserInfoService.layer >+>
    LiveJWTAuth.layer >+>
    LivePermissions.layer >+>
    LiveSys.layer >+>
    LivePython.layer >+>
    LiveFormsService.layer >+>
    PythonUDFRunner.layer(4) >+>
    LiveFlowRunner.layer >+>
    LiveFlowService.layer

}
