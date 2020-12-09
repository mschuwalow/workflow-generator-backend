package app

import app.domain._
import zio._
import app.repository.WorkflowRepository

object WorkflowManager {

  trait Service extends Serializable {
    def add(graph: raw.Graph): Task[typed.FlowWithId]
    def check(graph: raw.Graph): Task[typed.Flow]
    def delete(id: FlowId): Task[Unit]
  }

  val live: ZLayer[Interpreter with WorkflowRepository, Throwable, WorkflowManager] =
    ZLayer.fromManaged {
      ZManaged.environment[Interpreter with WorkflowRepository].flatMap { env =>
        Ref
          .make(Map.empty[FlowId, Fiber[Throwable, Unit]])
          .toManaged {
            _.get.flatMap(state => ZIO.foreach(state.values)(_.interrupt))
          }
          .flatMap { state =>
            def run(flow: typed.FlowWithId) =
              Promise.make[Nothing, Unit].flatMap { latch =>
                val task = (latch.await *> Interpreter.run(flow.flow)).foldM(
                  e =>
                    WorkflowRepository
                      .setState(flow.id, FlowState.Failed(e.getMessage())),
                  _ => WorkflowRepository.setState(flow.id, FlowState.Done)
                ) *> state.update(_ - flow.id)
                task.fork
                  .flatMap(f => state.update(_ + (flow.id -> f)) *> latch.succeed(()))
              }
            for {
              all <- WorkflowRepository.getAll
                       .map(_.filter(_.state == FlowState.Running))
                       .toManaged_
              _ <- ZIO.foreach_(all)(run).toManaged_
            } yield new Service {
              def add(graph: raw.Graph): Task[typed.FlowWithId] = {
                for {
                  flow <- check(graph)
                  flow <- WorkflowRepository.save(flow)
                  _    <- run(flow)
                } yield flow
              }.provide(env)

              def check(graph: raw.Graph): Task[typed.Flow] = {
                import app.compiler._
                val checked = for {
                  graph <- syntactic.checkCycles(graph)
                  flow  <- semantic.typecheck(graph)
                } yield flow
                checked.fold(e => ZIO.fail(Error.ValidationFailed(e)), ZIO.succeed(_))
              }

              def delete(id: FlowId): Task[Unit] = {
                for {
                  f <- state.modify(old => (old.get(id), old - id))
                  _ <- f.fold[IO[Throwable, Unit]](ZIO.fail(Error.NotFound))(
                         _.interrupt.unit
                       )
                  _ <- WorkflowRepository.delete(id)
                } yield ()
              }.provide(env)
            }
          }
      }
    }

  def add(graph: raw.Graph): RIO[WorkflowManager, typed.FlowWithId] =
    ZIO.accessM(_.get.add(graph))

  def check(graph: raw.Graph): RIO[WorkflowManager, typed.Flow] =
    ZIO.accessM(_.get.check(graph))

  def delete(id: FlowId): RIO[WorkflowManager, Unit] = ZIO.accessM(_.get.delete(id))

}
