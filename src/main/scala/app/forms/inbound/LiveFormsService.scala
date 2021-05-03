package app.forms.inbound

import app.Type
import app.forms._
import app.forms.outbound._
import zio._
import zio.stream.Stream

private final class LiveFormsService(
  env: LiveFormsService.Env
) extends FormsService {

  def create(request: CreateFormRequest): Task[Form] = {
    for {
      form <- FormsRepository.create(request)
      _    <- FormStreams.createStream(form.id)
    } yield form
  }.provide(env)

  def getById(id: app.forms.FormId): Task[Form] =
    FormsRepository
      .getById(id)
      .provide(env)

  def publish(form: Form)(element: form.outputType.Scala): UIO[Unit] = ???

  def subscribe(formId: FormId, elementType: Type): Stream[Nothing, elementType.Scala] = ???

}

private[inbound] object LiveFormsService {
  type Env = Has[FormsRepository] with Has[FormStreams]

  val layer: URLayer[Env, Has[FormsService]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveFormsService(env)
  }.toLayer
}
