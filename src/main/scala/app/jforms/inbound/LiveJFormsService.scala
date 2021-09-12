package app.jforms.inbound

import app.jforms._
import app.jforms.outbound._
import zio._
import zio.stream.Stream

private final class LiveJFormsService(
  env: LiveJFormsService.Env
) extends JFormsService {
  def create(request: CreateJFormRequest): Task[JForm] = {
    for {
      form <- JFormsRepository.create(request)
      _    <- JFormStreams.createStream(form.id)
    } yield form
  }.provide(env)

  def getById(id: JFormId): UIO[JForm] =
    JFormsRepository
      .getById(id)
      .provide(env)
      .orDie

  def submit(form: JForm)(element: form.outputType.Scala): UIO[Unit] =
    JFormStreams.publish(form)(Chunk.single(element)).provide(env)

  def subscribe(form: JForm): Stream[Nothing, form.outputType.Scala] =
    JFormStreams.consumeAll(form).provide(env)

}

private[inbound] object LiveJFormsService {
  type Env = Has[JFormsRepository] with Has[JFormStreams]

  val layer: URLayer[Env, Has[JFormsService]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveJFormsService(env)
  }.toLayer
}
