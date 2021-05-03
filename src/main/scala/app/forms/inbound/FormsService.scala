package app.forms.inbound

import app.Type
import app.forms._
import zio._
import zio.stream._

trait FormsService {
  def create(request: CreateFormRequest): Task[Form]
  def getById(id: FormId): Task[Form]
  def publish(form: Form)(element: form.outputType.Scala): UIO[Unit]
  def subscribe(formId: FormId, elementType: Type): Stream[Nothing, elementType.Scala]
}

object FormsService {

  def create(form: CreateFormRequest): RIO[Has[FormsService], Form] =
    ZIO.accessM(_.get.create(form))

  def getById(id: FormId): RIO[Has[FormsService], Form] =
    ZIO.accessM(_.get.getById(id))

  def publish(form: Form)(element: form.outputType.Scala): URIO[Has[FormsService], Unit] =
    ZIO.accessM(_.get.publish(form)(element))

  def subscribe(formId: FormId, elementType: Type): ZStream[Has[FormsService], Nothing, elementType.Scala] =
    ZStream.accessStream(_.get.subscribe(formId, elementType))

}
