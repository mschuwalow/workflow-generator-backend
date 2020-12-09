package app.repository

import zio._
import app.Database
import app.domain._
import doobie.implicits._
import zio.interop.catz._
import app.domain.typed.FlowWithId
import app.Error

object WorkflowRepository {

  trait Service extends Serializable {
    def save(flow: typed.Flow): Task[typed.FlowWithId]
    def getById(id: FlowId): Task[typed.FlowWithId]
    def getAll: Task[List[typed.FlowWithId]]

    def setState(
      id: FlowId,
      state: FlowState
    ): Task[Unit]
    def delete(id: FlowId): Task[Unit]
  }

  val doobie: ZLayer[Database, Nothing, WorkflowRepository] = ZLayer.fromServiceM {
    _.getTransactor.map { xa =>
      new Service {
        def save(flow: typed.Flow): Task[typed.FlowWithId] = {
          val query =
            sql"""INSERT INTO workflows (flow, state)
                 |VALUES ($flow, ${FlowState.Running: FlowState})""".stripMargin

          query.update
            .withUniqueGeneratedKeys[FlowId]("id")
            .map(id => typed.FlowWithId(id, flow, FlowState.Running))
            .transact(xa)
        }

        def getById(id: FlowId): Task[typed.FlowWithId] = {
          val query =
            sql"SELECT id, flow, state FROM workflows WHERE id = $id"
          query.query[FlowWithId].option.transact(xa).flatMap {
            case Some(flow) => ZIO.succeed(flow)
            case None       => ZIO.fail(Error.NotFound)
          }
        }

        val getAll: Task[List[typed.FlowWithId]] = {
          val query =
            sql"SELECT id, flow, state FROM workflows"
          query.query[FlowWithId].to[List].transact(xa)
        }

        def setState(
          id: FlowId,
          state: FlowState
        ): Task[Unit] = {
          val query =
            sql"UPDATE workflows SET state = $state WHERE id = $id"
          query.update.run.transact(xa).filterOrFail(_ > 0)(Error.NotFound).unit
        }

        def delete(id: FlowId): Task[Unit] = {
          val query =
            sql"DELETE FROM workflows WHERE id = $id"
          query.update.run.transact(xa).filterOrFail(_ > 0)(Error.NotFound).unit
        }
      }
    }
  }

  def save(flow: typed.Flow): RIO[WorkflowRepository, typed.FlowWithId] =
    ZIO.accessM(_.get.save(flow))

  def getById(id: FlowId): RIO[WorkflowRepository, typed.FlowWithId] =
    ZIO.accessM(_.get.getById(id))

  val getAll: RIO[WorkflowRepository, List[typed.FlowWithId]] =
    ZIO.accessM(_.get.getAll)

  def setState(
    id: FlowId,
    state: FlowState
  ): RIO[WorkflowRepository, Unit] = ZIO.accessM(_.get.setState(id, state))

  def delete(id: FlowId): RIO[WorkflowRepository, Unit] = ZIO.accessM(_.get.delete(id))
}
