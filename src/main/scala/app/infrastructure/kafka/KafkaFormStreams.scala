package app.infrastructure.kafka

import app.forms.outbound.FormStreams
import app.forms.{Form, FormId}
import zio._
import zio.stream._

private final class KafkaFormStreams(
  client: KafkaClient
) extends FormStreams {

  def createStream(id: FormId): UIO[Unit] =
    client.createStream(topicForForm(id))

  def deleteStream(id: FormId): UIO[Unit] =
    client.deleteStream(topicForForm(id))

  def publish(form: Form)(elements: Chunk[form.outputType.Scala]): UIO[Unit] =
    client.publishToStream(topicForForm(form.id), form.outputType)(elements)

  def consumeAll(form: Form): Stream[Nothing, form.outputType.Scala] =
    client.consumeStream(topicForForm(form.id), form.outputType).map(_.value)

  private def topicForForm(formId: FormId): String                   =
    s"forms-${formId.value}"

}

object KafkaFormStreams {
  type Env = Has[KafkaClient]

  val layer: ZLayer[Env, Nothing, Has[FormStreams]] =
    ZIO.access[Env](env => new KafkaFormStreams(env.get)).toLayer
}
