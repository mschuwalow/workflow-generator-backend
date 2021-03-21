package app.kafka

import app.config.KafkaConfig
import app.flows.{Committable, StreamsManager, Type}
import io.circe.parser.{parse => parseJson}
import io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.admin.{AdminClient, AdminClientSettings}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream

private final class KafkaStreamsManager(
  adminClient: AdminClient,
  producer: Producer.Service[Any, Int, String],
  env: KafkaStreamsManager.Env
) extends StreamsManager {

  def createStream(topicName: String) =
    adminClient
      .createTopic(AdminClient.NewTopic(topicName, 1, 1))
      .unlessM(topicExists(topicName))
      .provide(env)
      .orDie

  def deleteStream(topicName: String) =
    adminClient
      .deleteTopic(topicName)
      .whenM(topicExists(topicName))
      .provide(env)
      .orDie

  def publishToStream(topicName: String, elementType: Type)(elements: Chunk[elementType.Scala]) = {
    implicit val encoder = elementType.deriveEncoder
    for {
      awaitBatch <- ZIO.foreach(elements)(e => producer.produceAsync(topicName, 0, e.asJson.noSpaces))
      _          <- ZIO.collectAll_(awaitBatch)
    } yield ()
  }.provide(env).orDie

  def consumeStream(topicName: String, elementType: Type, consumerId: Option[String]) = {
    implicit val decoder = elementType.deriveDecoder

    ZStream
      .managed(makeConsumer(consumerId))
      .flatMap { consumer =>
        consumer
          .subscribeAnd(Subscription.topics(topicName))
          .plainStream(Serde.int, Serde.string)
          .mapM(c =>
            ZIO
              .fromEither(parseJson(c.value).flatMap(_.as[elementType.Scala]))
              .map(e => Committable(e, c.offset.commit.orDie))
          )
      }
      .refineOrDie(PartialFunction.empty)
  }.provide(env)

  def topicExists(topicName: String) =
    adminClient.describeTopics(List(topicName)).as(false).catchSome {
      case _: UnknownTopicOrPartitionException => ZIO.succeed(true)
    }

  def makeConsumer(groupId: Option[String]) =
    for {
      config           <- KafkaConfig.get.toManaged_
      settings          =
        ConsumerSettings(config.bootstrapServers).withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      settingsWithGroup = groupId.fold(settings)(settings.withGroupId)
      consumer         <- Consumer.make(settingsWithGroup)
    } yield consumer
}

object KafkaStreamsManager {
  type Env = Has[KafkaConfig] with Blocking with Clock

  val layer: ZLayer[Env, Throwable, Has[StreamsManager]] =
    ZLayer.fromManaged {
      val makeAdminClient = for {
        config <- KafkaConfig.get.toManaged_
        client <- AdminClient.make(AdminClientSettings(config.bootstrapServers))
      } yield client

      val makeProducer = for {
        config   <- KafkaConfig.get.toManaged_
        settings  = ProducerSettings(config.bootstrapServers)
                      .withProperty(ProducerConfig.LINGER_MS_CONFIG, java.lang.Long.valueOf(config.producerLingerMillis))
        producer <- Producer.make(settings, Serde.int, Serde.string)
      } yield producer

      for {
        env         <- ZManaged.environment[Env]
        adminClient <- makeAdminClient
        producer    <- makeProducer
      } yield new KafkaStreamsManager(adminClient, producer, env)
    }
}
