package app.flows.kafka

import app.config.KafkaConfig
import app.flows.Type
import io.circe.Codec
import io.circe.parser.{parse => parseJson}
import io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerRecord
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.admin.{AdminClient, AdminClientSettings}
import zio.kafka.consumer.{CommittableRecord, Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream._

import scala.annotation.nowarn

object KafkaStreamsManager {

  trait Service {
    def createStream(topicName: String): UIO[Unit]

    def deleteStream(topicName: String): UIO[Unit]

    def publishToStream(topicName: String, elementType: Type)(elements: Chunk[elementType.Scala])(implicit
      ev: Codec[elementType.Scala]
    ): UIO[Unit]

    def consumeStream(topicName: String, elementType: Type)(implicit
      ev: Codec[elementType.Scala]
    ): Stream[Nothing, CommittableRecord[Int, elementType.Scala]]
  }

  val live: ZLayer[KafkaConfig with Blocking with Clock, Throwable, KafkaStreamsManager] =
    ZLayer.fromFunctionManaged { env =>
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

      val make = for {
        adminClient <- makeAdminClient
        consumer    <- makeConsumer
        producer    <- makeProducer
      } yield new Service {
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

        @nowarn
        def consumeStream(topicName: String, elementType: Type)(implicit ev: Codec[elementType.Scala]) =
          consumer
            .subscribeAnd(Subscription.topics(topicName))
            .plainStream(Serde.int, Serde.string)
            .mapM(c =>
              ZIO
                .fromEither(parseJson(c.value).flatMap(_.as[elementType.Scala]))
                .map(e =>
                  CommittableRecord(
                    new ConsumerRecord(
                      c.record.topic(),
                      c.record.partition(),
                      c.record.offset(),
                      c.record.timestamp(),
                      c.record.timestampType,
                      c.record.checksum(),
                      c.record.serializedKeySize(),
                      c.record.serializedValueSize(),
                      c.record.key(),
                      e,
                      c.record.headers(),
                      c.record.leaderEpoch()
                    ),
                    c.offset
                  )
                )
            )
            .refineOrDie(PartialFunction.empty)
            .provide(env)
      }

      make.provide(env)
    }

  def createStream(topicName: String): URIO[KafkaStreamsManager, Unit] =
    ZIO.accessM(_.get.createStream(topicName))

  def deleteStream(topicName: String): URIO[KafkaStreamsManager, Unit] =
    ZIO.accessM(_.get.deleteStream(topicName))

  def publishToStream(topicName: String, elementType: Type)(elements: Chunk[elementType.Scala])(implicit
    ev: Codec[elementType.Scala]
  ): URIO[KafkaStreamsManager, Unit] =
    ZIO.accessM(_.get.publishToStream(topicName, elementType)(elements))

  def consumeStream(topicName: String, elementType: Type)(implicit
    ev: Codec[elementType.Scala]
  ): ZStream[KafkaStreamsManager, Nothing, CommittableRecord[Int, elementType.Scala]] =
    ZStream.accessStream(_.get.consumeStream(topicName, elementType))

}
