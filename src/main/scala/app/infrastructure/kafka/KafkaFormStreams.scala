package app.infrastructure.kafka

import app.forms.outbound.FormStreams
import zio._
import zio.stream._
import app.forms.{Form, FormId}

private final class KafkaFormStreams(
  client: KafkaClient
) extends FormStreams {

  def createStream(id: FormId): UIO[Unit] =
    client.createStream(topicForForm(id))

  def deleteStream(id: FormId): UIO[Unit] =
    client.deleteStream(topicForForm(id))

  def publish(form: Form)(elements: Chunk[form.outputType.Scala]): UIO[Unit] = ???

  def consumeAll(form: Form): Stream[Nothing, form.outputType.Scala] = ???

  private def topicForForm(formId: FormId): String =
    s"forms-${formId.value}"

 }

object KafkaFormStreams {
  type Env = LiveKafkaClient.Env

  val layer: ZLayer[Env, Throwable, Has[FormStreams]] = ???
}
