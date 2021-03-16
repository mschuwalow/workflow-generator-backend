package app.flows.kafka

import app.config.KafkaConfig
import app.flows.{Committable, StreamsManager, Type}
import io.circe.Codec
import io.circe.parser.{parse => parseJson}
import io.circe.syntax._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.admin.{AdminClient, AdminClientSettings}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde

private final class KafkaStreamsManager(
  adminClient: AdminClient,
  consumer: Consumer.Service,
  producer: Producer.Service[Any, Int, String],
  env: Blocking with Clock
) extends StreamsManager {

  def createStream(topicName: String) = {
    for {
      topics <- adminClient.describeTopics(List(topicName))
      _      <- adminClient.createTopic(AdminClient.NewTopic(topicName, 1, 1)).when(topics.isEmpty)
    } yield ()
  }.provide(env).orDie

  def deleteStream(topicName: String) = {
    for {
      topics <- adminClient.describeTopics(List(topicName))
      _      <- adminClient.deleteTopic(topicName).when(topics.nonEmpty)
    } yield ()
  }.provide(env).orDie

  def publishToStream(topicName: String, elementType: Type)(elements: Chunk[elementType.Scala])(implicit
    ev: Codec[elementType.Scala]
  ) = {
    for {
      awaitBatch <- ZIO.foreach(elements)(e => producer.produceAsync(topicName, 0, e.asJson.noSpaces))
      _          <- ZIO.collectAll_(awaitBatch)
    } yield ()
  }.provide(env).orDie

  def consumeStream(topicName: String, elementType: Type)(implicit ev: Codec[elementType.Scala]) =
    consumer
      .subscribeAnd(Subscription.topics(topicName))
      .plainStream(Serde.int, Serde.string)
      .mapM(c =>
        ZIO
          .fromEither(parseJson(c.value).flatMap(_.as[elementType.Scala]))
          .map(e => Committable(e))
      )
      .refineOrDie(PartialFunction.empty)
      .provide(env)

}

object KafkaStreamsManager {

  val layer: ZLayer[KafkaConfig with Blocking with Clock, Throwable, Has[StreamsManager]] =
    ZLayer.fromManaged {
      val makeAdminClient = for {
        config <- KafkaConfig.get.toManaged_
        client <- AdminClient.make(AdminClientSettings(config.bootstrapServers))
      } yield client

      val makeConsumer = for {
        config   <- KafkaConfig.get.toManaged_
        consumer <- Consumer.make(ConsumerSettings(config.bootstrapServers))
      } yield consumer

      val makeProducer = for {
        config   <- KafkaConfig.get.toManaged_
        producer <- Producer.make(ProducerSettings(config.bootstrapServers), Serde.int, Serde.string)
      } yield producer

      for {
        env         <- ZManaged.environment[Blocking with Clock]
        adminClient <- makeAdminClient
        consumer    <- makeConsumer
        producer    <- makeProducer
      } yield new KafkaStreamsManager(adminClient, consumer, producer, env)
    }
}
