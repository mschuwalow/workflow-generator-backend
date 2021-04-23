package app.flows

import zio._

trait FlowService {
  def add(graph: unresolved.CreateFlowRequest): Task[typed.Flow]
  def check(graph: unresolved.CreateFlowRequest): Task[typed.CreateFlowRequest]
  def delete(id: FlowId): Task[Unit]
}

object FlowService {

  def add(graph: unresolved.CreateFlowRequest): RIO[Has[FlowService], typed.Flow] =
    ZIO.accessM(_.get.add(graph))

  def check(graph: unresolved.CreateFlowRequest): RIO[Has[FlowService], typed.CreateFlowRequest] =
    ZIO.accessM(_.get.check(graph))

  def delete(id: FlowId): RIO[Has[FlowService], Unit] = ZIO.accessM(_.get.delete(id))

}
