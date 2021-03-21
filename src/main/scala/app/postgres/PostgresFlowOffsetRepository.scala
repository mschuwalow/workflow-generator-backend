package app.postgres

import app.flows.{ComponentId, FlowId, FlowOffset, FlowOffsetRepository}
import doobie.implicits._
import doobie.postgres.implicits._
import zio._
import zio.interop.catz._

final class PostgresFlowOffsetRepository(xa: TaskTransactor) extends FlowOffsetRepository with MetaInstances {

  def put(offset: FlowOffset) = {
    val query =
      sql"""INSERT INTO flow_offsets (flow_id, component_id, stream_offset)
           |VALUES (${offset.flowId}, ${offset.componentId}, ${offset.offset})
           |ON CONFLICT (flow_id, component_id) DO UPDATE
           |SET stream_offset = EXCLUDED.stream_offset
           |""".stripMargin

    query.update.run
      .transact(xa)
      .unit
      .orDie
  }

  def get(flowId: FlowId, componentId: ComponentId) = {
    val query =
      sql"""SELECT flow_id, component_id, stream_offset
           |FROM flow_offsets
           |WHERE flow_id = $flowId
           |      AND component_id = $componentId
           |""".stripMargin
    query
      .query[FlowOffset]
      .option
      .transact(xa)
      .orDie
  }
}

object PostgresFlowOffsetRepository {
  type Env = Has[Database]

  val layer: URLayer[Env, Has[FlowOffsetRepository]] =
    Database.getTransactor.map(new PostgresFlowOffsetRepository(_)).toLayer

}
