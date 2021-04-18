package app.flows

import app.Error
import app.forms.FormsRepository
import zio._

private final class LiveFlowService(
  state: LiveFlowService.internal.State,
  env: LiveFlowService.Env
) extends FlowService {
  import LiveFlowService.internal._

  def add(graph: unresolved.Graph) = {
    for {
      flow <- check(graph)
      flow <- FlowRepository.save(flow)
      _    <- run(state, flow)
    } yield flow
  }.provide(env)

  def check(graph: unresolved.Graph) = {
    for {
      graph <- ZIO.fromEither(compiler.syntactic.checkCycles(graph))
      graph <- compiler.resolver.resolve(graph)
      flow  <- ZIO.fromEither(compiler.semantic.typecheck(graph))
    } yield flow
  }.provide(env)

  def delete(id: FlowId) = {
    for {
      f <- state.modify(old => (old.get(id), old - id))
      _ <- f.fold[IO[Throwable, Unit]](ZIO.fail(Error.NotFound))(
             _.interrupt.unit
           )
      _ <- FlowRepository.delete(id)
    } yield ()
  }.provide(env)
}

object LiveFlowService {

  type Env = Has[FlowRunner] with Has[FlowRepository] with Has[FormsRepository]

  val layer: ZLayer[Env, Throwable, Has[FlowService]] = {
    for {
      env     <- ZManaged.environment[Env]
      state   <- Ref.make(Map.empty[FlowId, Fiber[Throwable, Unit]]).toManaged {
                   _.get.flatMap(state => ZIO.foreach(state.values)(_.interrupt))
                 }
      running <- FlowRepository.getAll
                   .map(_.filter(_.state == FlowState.Running))
                   .toManaged_
      _       <- ZIO.foreach_(running)(internal.run(state, _)).toManaged_
    } yield new LiveFlowService(state, env)
  }.toLayer

  private object internal {
    type State = Ref[Map[FlowId, Fiber[Throwable, Unit]]]

    def run(state: State, flow: typed.FlowWithId) =
      ZIO.uninterruptibleMask { restore =>
        for {
          latch <- Promise.make[Nothing, Unit]
          task   = restore {
                     latch.await *>
                       FlowRunner
                         .run(flow)
                         .foldM(
                           e => FlowRepository.setState(flow.id, FlowState.Failed(e.getMessage())),
                           _ => FlowRepository.setState(flow.id, FlowState.Done)
                         )
                         .ensuring(state.update(_ - flow.id))
                   }
          f     <- task.fork
          _     <- state.update(_ + (flow.id -> f))
          _     <- latch.succeed(())
        } yield ()
      }
  }
}
