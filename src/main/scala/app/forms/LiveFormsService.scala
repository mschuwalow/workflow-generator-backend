package app.forms

import zio._
import app.flows.StreamsManager
import app.flows.StreamsManager.topicForForm

private final class LiveFormsService(
  env: LiveFormsService.Env
) extends FormsService {

  def create(form: Form): Task[FormWithId] = {
    for {
      withId <- FormsRepository.store(form)
      _      <- StreamsManager.createStream(topicForForm(withId.id))
    } yield withId
  }.provide(env)

}

object LiveFormsService {
  type Env = Has[FormsRepository] with Has[StreamsManager]

  val layer: URLayer[Env, Has[FormsService]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveFormsService(env)
  }.toLayer
}
