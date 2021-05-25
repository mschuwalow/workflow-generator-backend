package app.forms.inbound

import app.forms._
import zio._
import zio.stream._

trait FormsService {
  def create(request: CreateFormRequest): Task[Form]
  def getById(id: FormId): UIO[Form]
  def submit(form: Form)(result: form.outputType.Scala): UIO[Unit]
  def subscribe(form: Form): Stream[Nothing, form.outputType.Scala]
}

object FormsService {

  def create(form: CreateFormRequest): RIO[Has[FormsService], Form] =
    ZIO.accessM(_.get.create(form))

  def getById(id: FormId): URIO[Has[FormsService], Form] =
    ZIO.accessM(_.get.getById(id))

  def publish(form: Form)(element: form.outputType.Scala): URIO[Has[FormsService], Unit] =
    ZIO.accessM(_.get.submit(form)(element))

  def subscribe(form: Form): ZStream[Has[FormsService], Nothing, form.outputType.Scala] =
    ZStream.accessStream(_.get.subscribe(form))

}
