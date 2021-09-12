package app

import app.config.ConfigLayer
import app.infrastructure.http.StudIpUserInfoService
import app.infrastructure.kafka._
import app.infrastructure.postgres._
import app.infrastructure.udf.LiveUDFRunner
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
      PostgresJFormsRepository.layer >+>
      LiveUDFRunner.layer(4) >+>
      LiveKafkaClient.layer >+>
      KafkaFormStreams.layer >+>
      KafkaJFormStreams.layer >+>
      HttpClientZioBackend.layer() >+>
      StudIpUserInfoService.layer >+>
      app.auth.inbound.layer >+>
      app.forms.inbound.layer >+>
      app.jforms.inbound.layer >+>
      app.flows.inbound.layer
}
