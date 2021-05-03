package app.forms.inbound

import app.forms._
import app.forms.outbound._
import zio._

private final class LiveFormsService(
  env: LiveFormsService.Env
) extends FormsService {

  def create(request: CreateFormRequest): Task[Form] = {
    for {
      form   <- FormsRepository.create(request)
      _      <- FormStreams.createStream(form.id)
    } yield form
  }.provide(env)

}

object LiveFormsService {
  type Env = Has[FormsRepository] with Has[FormStreams]

  val layer: URLayer[Env, Has[FormsService]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveFormsService(env)
  }.toLayer
}
