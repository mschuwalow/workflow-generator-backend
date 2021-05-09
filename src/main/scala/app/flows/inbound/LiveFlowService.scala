package app.flows.inbound

import app.Error
import app.flows._
import app.flows.inbound.compiler
import app.flows.outbound._
import app.forms.inbound.FormsService
import zio._

private final class LiveFlowService(
  state: LiveFlowService.internal.State,
  env: LiveFlowService.Env
) extends FlowService {
  import LiveFlowService.internal._

  def add(request: unresolved.CreateFlowRequest) = {
    for {
      request <- check(request)
      flow    <- FlowRepository.create(request)
      _       <- forkRun(state, flow)
    } yield flow
  }.provide(env)

  def check(graph: unresolved.CreateFlowRequest) = {
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

  def getById(id: FlowId) =
    FlowRepository.getById(id).provide(env)
}

private[inbound] object LiveFlowService {

  type Env = Has[FlowRunner] with Has[FlowRepository] with Has[FormsService]

  val layer: ZLayer[Env, Throwable, Has[FlowService]] = {
    for {
      env     <- ZManaged.environment[Env]
      state   <- Ref.make(Map.empty[FlowId, Fiber[Throwable, Unit]]).toManaged {
                   _.get.flatMap(state => ZIO.foreach(state.values)(_.interrupt))
                 }
      running <- FlowRepository.getAll
                   .map(_.filter(_.state == FlowState.Running))
                   .toManaged_
      _       <- ZIO.foreach_(running)(internal.forkRun(state, _)).toManaged_
    } yield new LiveFlowService(state, env)
  }.toLayer

  private object internal {
    type State = Ref[Map[FlowId, Fiber[Throwable, Unit]]]

    def forkRun(state: State, flow: typed.Flow) =
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
