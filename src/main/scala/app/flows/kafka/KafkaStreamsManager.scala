package app.flows.kafka

import app.config.KafkaConfig
import app.flows.{ComponentId, FlowId}
import app.forms.FormId
import zio._
import zio.blocking.Blocking
import zio.kafka.admin.{AdminClient, AdminClientSettings}

object KafkaStreamsManager {

  trait Service {
    def createStreamForForm(formId: FormId): UIO[Unit]
    def createStreamForFlow(flowId: FlowId, componentId: ComponentId): UIO[Unit]
    def deleteStreamForForm(formId: FormId): UIO[Unit]
    def deleteStreamForFlow(flowId: FlowId, componentId: ComponentId): UIO[Unit]
  }

  val live: ZLayer[KafkaConfig with Blocking, Throwable, KafkaStreamsManager] =
    ZLayer.fromFunctionManaged { (env: KafkaConfig with Blocking) =>
      val makeAdminClient = for {
        config <- KafkaConfig.get.toManaged_
        client <- AdminClient.make(AdminClientSettings(config.bootstrapServers))
      } yield client

      makeAdminClient.map { adminClient =>
        new Service {
          def createStreamForForm(formId: FormId) = {
            val name = topicForForm(formId)

            for {
              topics <- adminClient.describeTopics(List(name))
              _      <- adminClient.createTopic(AdminClient.NewTopic(name, 1, 1)).when(topics.isEmpty)
            } yield ()
          }.provide(env).orDie

          def createStreamForFlow(flowId: FlowId, componentId: ComponentId) = {
            val name = topicForFlow(flowId, componentId)

            for {
              topics <- adminClient.describeTopics(List(name))
              _      <- adminClient.createTopic(AdminClient.NewTopic(name, 1, 1)).when(topics.isEmpty)
            } yield ()
          }.provide(env).orDie

          def deleteStreamForForm(formId: FormId) = {
            val name = topicForForm(formId)

            for {
              topics <- adminClient.describeTopics(List(name))
              _      <- adminClient.deleteTopic(name).when(topics.nonEmpty)
            } yield ()
          }.provide(env).orDie

          def deleteStreamForFlow(flowId: FlowId, componentId: ComponentId) = {
            val name = topicForFlow(flowId, componentId)

            for {
              topics <- adminClient.describeTopics(List(name))
              _      <- adminClient.deleteTopic(name).when(topics.nonEmpty)
            } yield ()
          }.provide(env).orDie
        }
      }.provide(env)
    }

  def createStreamForForm(formId: FormId): URIO[KafkaStreamsManager, Unit] =
    ZIO.accessM(_.get.createStreamForForm(formId))

  def createStreamForFlow(flowId: FlowId, componentId: ComponentId): URIO[KafkaStreamsManager, Unit] =
    ZIO.accessM(_.get.createStreamForFlow(flowId, componentId))

  def deleteStreamForForm(formId: FormId): URIO[KafkaStreamsManager, Unit] =
    ZIO.accessM(_.get.deleteStreamForForm(formId))

  def deleteStreamForFlow(flowId: FlowId, componentId: ComponentId): URIO[KafkaStreamsManager, Unit] =
    ZIO.accessM(_.get.deleteStreamForFlow(flowId, componentId))

}
