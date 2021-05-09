package app.forms.outbound

import app.forms.{FormId, _}
import zio._
import zio.stream._

trait FormStreams {
  def createStream(id: FormId): UIO[Unit]
  def deleteStream(id: FormId): UIO[Unit]
  def publish(form: Form)(elements: Chunk[form.outputType.Scala]): UIO[Unit]
  def consumeAll(form: Form): Stream[Nothing, form.outputType.Scala]
}

object FormStreams {

  def createStream(id: FormId): URIO[Has[FormStreams], Unit] =
    ZIO.accessM(_.get.createStream(id))

  def deleteStream(id: FormId): URIO[Has[FormStreams], Unit] =
    ZIO.accessM(_.get.deleteStream(id))

  def publish(form: Form)(elements: Chunk[form.outputType.Scala]): URIO[Has[FormStreams], Unit] =
    ZIO.accessM(_.get.publish(form)(elements))

  def consumeAll(form: Form): ZStream[Has[FormStreams], Nothing, form.outputType.Scala] =
    ZStream.accessStream(_.get.consumeAll(form))
}
