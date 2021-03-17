package app

// import app.api.Auth
// import app.auth.{Permissions, UserInfoService}
// import app.config._
// import app.flows.udf._
// import app.flows.{FlowRepository, FlowRunner, FlowService}
// import app.forms.FormsRepository
// import app.postgres.Database
// import sttp.client.httpclient.zio.HttpClientZioBackend
import zio._
// import zio.logging.slf4j.Slf4jLogger
// import app.flows.kafka.KafkaStreamsManager
// import app.postgres._
object layers {

  val prod: ZLayer[ZEnv, Throwable, AppEnvironment] = ???
    // ZLayer.identity[ZEnv] >+>
    //   AppConfig.live >+>
    //   Slf4jLogger.make((_, msg) => msg) >+>
    //   KafkaStreamsManager.layer >+>
    //   Permissions.live >+>
    //   HttpClientZioBackend.layer() >+>
    //   UserInfoService.live >+>
    //   Auth.live >+>
    //   Database.migrated >+>
    //   PostgresFlowRepository.layer >+>
    //   PostgresFormsRepository.layer >+>
    //   LiveSys.layer >+>
    //   LivePython.layer >+>
    //   PythonUDFRunner.layer(workers = 5) >+>
    //   FlowRunner.stream >+>
    //   FlowService.live
}
