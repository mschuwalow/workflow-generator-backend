package app.repository

import zio._
import app.Database
import app.domain._
import doobie.implicits._
import zio.interop.catz._
import app.domain.typed.FlowWithId

object WorkflowRepository {

  trait Service extends Serializable {
    def save(flow: typed.Flow): Task[typed.FlowWithId]
    def getById(id: FlowId): Task[Option[typed.FlowWithId]]
    def getAll: Task[List[typed.FlowWithId]]
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

        def getById(id: FlowId): Task[Option[typed.FlowWithId]] = {
          val query =
            sql"SELECT id, flow, state FROM workflows WHERE id = $id"
          query.query[FlowWithId].option.transact(xa)
        }

        val getAll: Task[List[typed.FlowWithId]] = {
          val query =
            sql"SELECT id, flow, state FROM workflows"
          query.query[FlowWithId].to[List].transact(xa)
        }
      }
    }
  }

  def save(flow: typed.Flow): RIO[WorkflowRepository, typed.FlowWithId] =
    ZIO.accessM(_.get.save(flow))

  def getById(id: FlowId): RIO[WorkflowRepository, Option[typed.FlowWithId]] =
    ZIO.accessM(_.get.getById(id))

  val getAll: RIO[WorkflowRepository, List[typed.FlowWithId]] =
    ZIO.accessM(_.get.getAll)
}
