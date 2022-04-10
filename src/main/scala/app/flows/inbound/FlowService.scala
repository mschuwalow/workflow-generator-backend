package app.flows.inbound

import app.flows._
import zio._

trait FlowService {
  def add(request: unresolved.CreateFlowRequest): Task[typed.Flow]
  def compile(request: unresolved.CreateFlowRequest): Task[typed.CreateFlowRequest]
  def getById(id: FlowId): Task[typed.Flow]
  def delete(id: FlowId): Task[Unit]
}

object FlowService {

  def add(graph: unresolved.CreateFlowRequest): RIO[Has[FlowService], typed.Flow] =
    ZIO.accessM(_.get.add(graph))

  def compile(graph: unresolved.CreateFlowRequest): RIO[Has[FlowService], typed.CreateFlowRequest] =
    ZIO.accessM(_.get.compile(graph))

  def getById(id: FlowId): RIO[Has[FlowService], typed.Flow] =
    ZIO.accessM(_.get.getById(id))

  def delete(id: FlowId): RIO[Has[FlowService], Unit] =
    ZIO.accessM(_.get.delete(id))

}
