package app.flows

import zio._

trait FlowService {
  def add(graph: unresolved.Graph): Task[typed.FlowWithId]
  def check(graph: unresolved.Graph): Task[typed.Flow]
  def delete(id: FlowId): Task[Unit]
}

object FlowService {

  def add(graph: unresolved.Graph): RIO[Has[FlowService], typed.FlowWithId] =
    ZIO.accessM(_.get.add(graph))

  def check(graph: unresolved.Graph): RIO[Has[FlowService], typed.Flow] =
    ZIO.accessM(_.get.check(graph))

  def delete(id: FlowId): RIO[Has[FlowService], Unit] = ZIO.accessM(_.get.delete(id))

}
