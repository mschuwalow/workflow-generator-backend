package app.postgres

import app.Error
import app.flows.typed.FlowWithId
import app.flows.{FlowId, FlowRepository, FlowState, typed}
import doobie.implicits._
import doobie.postgres.implicits._
import zio.interop.catz._
import zio.{Task, _}

final class PostgresFlowRepository(xa: TaskTransactor) extends FlowRepository with MetaInstances {

  def save(flow: typed.Flow): Task[typed.FlowWithId] = {
    val query =
      sql"""INSERT INTO flows (streams, state)
           |VALUES (${flow.streams}, ${FlowState.Running: FlowState})""".stripMargin

    query.update
      .withUniqueGeneratedKeys[FlowId]("flow_id")
      .map(id => typed.FlowWithId(id, flow.streams, FlowState.Running))
      .transact(xa)
  }

  def getById(id: FlowId): Task[typed.FlowWithId] = {
    val query =
      sql"SELECT flow_id, streams, state FROM flows WHERE flow_id = $id"
    query.query[FlowWithId].option.transact(xa).flatMap {
      case Some(flow) => ZIO.succeed(flow)
      case None       => ZIO.fail(Error.NotFound)
    }
  }

  val getAll: Task[List[typed.FlowWithId]] = {
    val query =
      sql"SELECT flow_id, streams, state FROM flows"
    query.query[FlowWithId].to[List].transact(xa)
  }

  def setState(
    id: FlowId,
    state: FlowState
  ): Task[Unit] = {
    val query =
      sql"UPDATE flows SET state = $state WHERE flow_id = $id"
    query.update.run.transact(xa).filterOrFail(_ > 0)(Error.NotFound).unit
  }

  def delete(id: FlowId): Task[FlowWithId] = {
    val query =
      sql"""DELETE FROM flows
           |WHERE flow_id = $id
           |RETURNING (flow_id, streams, state)""".stripMargin
    query.query[FlowWithId].option.transact(xa).someOrFail(Error.NotFound)
  }
}

object PostgresFlowRepository {
  type Env = Has[Database]

  val layer: URLayer[Env, Has[FlowRepository]] =
    Database.getTransactor.map(new PostgresFlowRepository(_)).toLayer

}
