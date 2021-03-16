package app.flows

import app.Error
import app.forms.FormsRepository
import zio._

object FlowService {

  trait Service extends Serializable {
    def add(graph: unresolved.Graph): Task[typed.FlowWithId]
    def check(graph: unresolved.Graph): Task[typed.Flow]
    def delete(id: FlowId): Task[Unit]
  }

  val live: ZLayer[FlowRunner with FlowRepository with FormsRepository, Throwable, FlowService] =
    ZLayer.fromManaged {
      ZManaged.environment[FlowRunner with FlowRepository with FormsRepository].flatMap { env =>
        Ref
          .make(Map.empty[FlowId, Fiber[Throwable, Unit]])
          .toManaged {
            _.get.flatMap(state => ZIO.foreach(state.values)(_.interrupt))
          }
          .flatMap { state =>
            def run(flow: typed.FlowWithId) =
              Promise.make[Nothing, Unit].flatMap { latch =>
                val task = (latch.await *> FlowRunner.run(flow)).foldM(
                  e =>
                    FlowRepository
                      .setState(flow.id, FlowState.Failed(e.getMessage())),
                  _ => FlowRepository.setState(flow.id, FlowState.Done)
                ) *> state.update(_ - flow.id)
                task.fork.flatMap(f => state.update(_ + (flow.id -> f)) *> latch.succeed(())).uninterruptible
              }
            for {
              all <- FlowRepository.getAll
                       .map(_.filter(_.state == FlowState.Running))
                       .toManaged_
              _ <- ZIO.foreach_(all)(run).toManaged_
            } yield new Service {
              def add(graph: unresolved.Graph): Task[typed.FlowWithId] = {
                for {
                  flow <- check(graph)
                  flow <- FlowRepository.save(flow)
                  _    <- run(flow)
                } yield flow
              }.provide(env)

              def check(graph: unresolved.Graph): Task[typed.Flow] = {
                for {
                  graph <- ZIO.fromEither(compiler.syntactic.checkCycles(graph))
                  graph <- compiler.resolver.resolve(graph)
                  flow  <- ZIO.fromEither(compiler.semantic.typecheck(graph))
                } yield flow
              }.provide(env)

              def delete(id: FlowId): Task[Unit] = {
                for {
                  f <- state.modify(old => (old.get(id), old - id))
                  _ <- f.fold[IO[Throwable, Unit]](ZIO.fail(Error.NotFound))(
                         _.interrupt.unit
                       )
                  _ <- FlowRepository.delete(id)
                } yield ()
              }.provide(env)
            }
          }
      }
    }

  def add(graph: unresolved.Graph): RIO[FlowService, typed.FlowWithId] =
    ZIO.accessM(_.get.add(graph))

  def check(graph: unresolved.Graph): RIO[FlowService, typed.Flow] =
    ZIO.accessM(_.get.check(graph))

  def delete(id: FlowId): RIO[FlowService, Unit] = ZIO.accessM(_.get.delete(id))

}
