package app.flows

import app.flows.Type
import app.forms.FormId
import zio._
import zio.stream._

trait StreamsManager {

  def createStream(topicName: String): UIO[Unit]

  def deleteStream(topicName: String): UIO[Unit]

  def publishToStream(topicName: String, elementType: Type)(elements: Chunk[elementType.Scala]): UIO[Unit]

  def consumeStream(
    topicName: String,
    elementType: Type,
    consumerId: String
  ): Stream[Nothing, Committable[elementType.Scala]]

}

object StreamsManager {

  def topicForForm(formId: FormId): String =
    s"forms-${formId.value}"

  def topicForFlow(flowId: FlowId, componentId: ComponentId): String =
    s"flow-${flowId.value}-${componentId.value}"

  def createStream(streamName: String): URIO[Has[StreamsManager], Unit] =
    ZIO.accessM(_.get.createStream(streamName))

  def deleteStream(streamName: String): URIO[Has[StreamsManager], Unit] =
    ZIO.accessM(_.get.deleteStream(streamName))

  def publishToStream(streamName: String, elementType: Type)(
    elements: Chunk[elementType.Scala]
  ): URIO[Has[StreamsManager], Unit] =
    ZIO.accessM(_.get.publishToStream(streamName, elementType)(elements))

  def consumeStream(
    streamName: String,
    elementType: Type,
    consumerId: String
  ): ZStream[Has[StreamsManager], Nothing, Committable[elementType.Scala]] =
    ZStream.accessStream(_.get.consumeStream(streamName, elementType, consumerId))

}
