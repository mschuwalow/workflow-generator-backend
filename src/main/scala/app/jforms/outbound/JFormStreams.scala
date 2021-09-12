package app.jforms.outbound

import app.jforms._
import zio._
import zio.stream._

trait JFormStreams {
  def createStream(id: JFormId): UIO[Unit]
  def deleteStream(id: JFormId): UIO[Unit]
  def publish(form: JForm)(elements: Chunk[form.outputType.Scala]): UIO[Unit]
  def consumeAll(form: JForm): Stream[Nothing, form.outputType.Scala]
}

object JFormStreams {

  def createStream(id: JFormId): URIO[Has[JFormStreams], Unit] =
    ZIO.accessM(_.get.createStream(id))

  def deleteStream(id: JFormId): URIO[Has[JFormStreams], Unit] =
    ZIO.accessM(_.get.deleteStream(id))

  def publish(form: JForm)(elements: Chunk[form.outputType.Scala]): URIO[Has[JFormStreams], Unit] =
    ZIO.accessM(_.get.publish(form)(elements))

  def consumeAll(form: JForm): ZStream[Has[JFormStreams], Nothing, form.outputType.Scala] =
    ZStream.accessStream(_.get.consumeAll(form))
}
