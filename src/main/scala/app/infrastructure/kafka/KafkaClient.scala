package app.infrastructure.kafka

import app.Type
import zio._
import zio.stream._

trait KafkaClient {

  def createStream(topicName: String): UIO[Unit]

  def deleteStream(topicName: String): UIO[Unit]

  def publishToStream(topicName: String, elementType: Type)(elements: Chunk[elementType.Scala]): UIO[Unit]

  def consumeStream(
    topicName: String,
    elementType: Type,
    consumerId: Option[String] = None
  ): Stream[Nothing, Committable[elementType.Scala]]

}

object KafkaClient {

  def createStream(streamName: String): URIO[Has[KafkaClient], Unit] =
    ZIO.accessM(_.get.createStream(streamName))

  def deleteStream(streamName: String): URIO[Has[KafkaClient], Unit] =
    ZIO.accessM(_.get.deleteStream(streamName))

  def publishToStream(streamName: String, elementType: Type)(
    elements: Chunk[elementType.Scala]
  ): URIO[Has[KafkaClient], Unit] =
    ZIO.accessM(_.get.publishToStream(streamName, elementType)(elements))

  def consumeStream(
    streamName: String,
    elementType: Type,
    consumerId: Option[String] = None
  ): ZStream[Has[KafkaClient], Nothing, Committable[elementType.Scala]] =
    ZStream.accessStream(_.get.consumeStream(streamName, elementType, consumerId))

}
