package app.infrastructure.kafka

import app.jforms.outbound.JFormStreams
import app.jforms.{JForm, JFormId}
import zio._
import zio.stream._

private final class KafkaJFormStreams(
  client: KafkaClient
) extends JFormStreams {

  def createStream(id: JFormId): UIO[Unit] =
    client.createStream(topicForForm(id))

  def deleteStream(id: JFormId): UIO[Unit] =
    client.deleteStream(topicForForm(id))

  def publish(form: JForm)(elements: Chunk[form.outputType.Scala]): UIO[Unit] =
    client.publishToStream(topicForForm(form.id), form.outputType)(elements)

  def consumeAll(form: JForm): Stream[Nothing, form.outputType.Scala] =
    client.consumeStream(topicForForm(form.id), form.outputType).map(_.value)

  private def topicForForm(formId: JFormId): String =
    s"jforms-${formId.value}"

}

object KafkaJFormStreams {
  type Env = Has[KafkaClient]

  val layer: URLayer[Env, Has[JFormStreams]] =
    ZIO.access[Env](env => new KafkaJFormStreams(env.get)).toLayer
}
