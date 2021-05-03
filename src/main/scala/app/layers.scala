package app

import app.config.ConfigLayer
import app.infrastructure.http.StudIpUserInfoService
import app.infrastructure.kafka.{KafkaFormStreams, LiveKafkaClient}
import app.infrastructure.postgres.{
  Database,
  PostgresFlowOffsetRepository,
  PostgresFlowRepository,
  PostgresFormsRepository
}
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
      LiveUDFRunner.layer(4) >+>
      LiveKafkaClient.layer >+>
      KafkaFormStreams.layer >+>
      HttpClientZioBackend.layer() >+>
      StudIpUserInfoService.layer >+>
      app.auth.inbound.layer >+>
      app.forms.inbound.layer >+>
      app.flows.inbound.layer
}
