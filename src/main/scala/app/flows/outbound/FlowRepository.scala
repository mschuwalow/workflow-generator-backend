package app.flows.outbound

import app.flows._
import zio._

trait FlowRepository {
  def create(flow: typed.CreateFlowRequest): Task[typed.Flow]
  def getById(id: FlowId): Task[typed.Flow]
  def getAll: Task[List[typed.Flow]]

  def setState(
    id: FlowId,
    state: FlowState
  ): Task[Unit]

  def delete(id: FlowId): Task[typed.Flow]
}

object FlowRepository {

  def create(flow: typed.CreateFlowRequest): RIO[Has[FlowRepository], typed.Flow] =
    ZIO.accessM(_.get.create(flow))

  def getById(id: FlowId): RIO[Has[FlowRepository], typed.Flow] =
    ZIO.accessM(_.get.getById(id))

  val getAll: RIO[Has[FlowRepository], List[typed.Flow]] =
    ZIO.accessM(_.get.getAll)

  def setState(
    id: FlowId,
    state: FlowState
  ): RIO[Has[FlowRepository], Unit] = ZIO.accessM(_.get.setState(id, state))

  def delete(id: FlowId): RIO[Has[FlowRepository], typed.Flow] = ZIO.accessM(_.get.delete(id))
}
