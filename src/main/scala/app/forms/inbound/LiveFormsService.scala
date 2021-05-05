package app.forms.inbound

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

  def getById(id: FormId): UIO[Form] =
    FormsRepository
      .getById(id)
      .provide(env)
      .orDie

  def publish(form: Form)(element: form.outputType.Scala): UIO[Unit] =
    FormStreams.publish(form)(Chunk.single(element)).provide(env)

  def subscribe(form: Form): Stream[Nothing, form.outputType.Scala] =
    FormStreams.consumeAll(form).provide(env)

}

private[inbound] object LiveFormsService {
  type Env = Has[FormsRepository] with Has[FormStreams]

  val layer: URLayer[Env, Has[FormsService]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveFormsService(env)
  }.toLayer
}
