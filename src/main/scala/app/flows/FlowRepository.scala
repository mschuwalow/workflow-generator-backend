package app.flows

import app.flows._
import zio._

trait FlowRepository {
  def save(flow: typed.Flow): Task[typed.FlowWithId]
  def getById(id: FlowId): Task[typed.FlowWithId]
  def getAll: Task[List[typed.FlowWithId]]

  def setState(
    id: FlowId,
    state: FlowState
  ): Task[Unit]

  def delete(id: FlowId): Task[Unit]
}

object FlowRepository {

  def save(flow: typed.Flow): RIO[Has[FlowRepository], typed.FlowWithId] =
    ZIO.accessM(_.get.save(flow))

  def getById(id: FlowId): RIO[Has[FlowRepository], typed.FlowWithId] =
    ZIO.accessM(_.get.getById(id))

  val getAll: RIO[Has[FlowRepository], List[typed.FlowWithId]] =
    ZIO.accessM(_.get.getAll)

  def setState(
    id: FlowId,
    state: FlowState
  ): RIO[Has[FlowRepository], Unit] = ZIO.accessM(_.get.setState(id, state))

  def delete(id: FlowId): RIO[Has[FlowRepository], Unit] = ZIO.accessM(_.get.delete(id))
}
