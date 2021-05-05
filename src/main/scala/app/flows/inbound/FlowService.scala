package app.flows.inbound

import app.flows._
import zio._

trait FlowService {
  def add(graph: unresolved.CreateFlowRequest): Task[typed.Flow]
  def check(graph: unresolved.CreateFlowRequest): Task[typed.CreateFlowRequest]
  def getById(id: FlowId): Task[typed.Flow]
  def delete(id: FlowId): Task[Unit]
}

object FlowService {

  def add(graph: unresolved.CreateFlowRequest): RIO[Has[FlowService], typed.Flow] =
    ZIO.accessM(_.get.add(graph))

  def check(graph: unresolved.CreateFlowRequest): RIO[Has[FlowService], typed.CreateFlowRequest] =
    ZIO.accessM(_.get.check(graph))

  def getById(id: FlowId): RIO[Has[FlowService], typed.Flow] =
    ZIO.accessM(_.get.getById(id))

  def delete(id: FlowId): RIO[Has[FlowService], Unit] =
    ZIO.accessM(_.get.delete(id))

}
