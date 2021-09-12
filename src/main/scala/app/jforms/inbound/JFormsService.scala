package app.jforms.inbound

import app.jforms._
import zio._
import zio.stream.{Stream, ZStream}

trait JFormsService {
  def create(request: CreateJFormRequest): Task[JForm]
  def getById(id: JFormId): UIO[JForm]
  def submit(form: JForm)(result: form.outputType.Scala): UIO[Unit]
  def subscribe(form: JForm): Stream[Nothing, form.outputType.Scala]
}

object JFormsService {

  def create(form: CreateJFormRequest): RIO[Has[JFormsService], JForm] =
    ZIO.accessM(_.get.create(form))

  def getById(id: JFormId): URIO[Has[JFormsService], JForm] =
    ZIO.accessM(_.get.getById(id))

  def publish(form: JForm)(element: form.outputType.Scala): URIO[Has[JFormsService], Unit] =
    ZIO.accessM(_.get.submit(form)(element))

  def subscribe(form: JForm): ZStream[Has[JFormsService], Nothing, form.outputType.Scala] =
    ZStream.accessStream(_.get.subscribe(form))

}
