package app.flows

import app.Error
import app.flows._
import app.flows.typed.FlowWithId
import app.postgres.{Database, MetaInstances}
import doobie.implicits._
import doobie.postgres.implicits._
import zio._
import zio.interop.catz._

object FlowRepository extends MetaInstances {

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

  val doobie: ZLayer[Database, Nothing, FlowRepository] = ZLayer.fromServiceM {
    _.getTransactor.map { xa =>
      new Service {
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

        def delete(id: FlowId): Task[Unit] = {
          val query =
            sql"DELETE FROM flows WHERE flow_id = $id"
          query.update.run.transact(xa).filterOrFail(_ > 0)(Error.NotFound).unit
        }
      }
    }
  }

  def save(flow: typed.Flow): RIO[FlowRepository, typed.FlowWithId] =
    ZIO.accessM(_.get.save(flow))

  def getById(id: FlowId): RIO[FlowRepository, typed.FlowWithId] =
    ZIO.accessM(_.get.getById(id))

  val getAll: RIO[FlowRepository, List[typed.FlowWithId]] =
    ZIO.accessM(_.get.getAll)

  def setState(
    id: FlowId,
    state: FlowState
  ): RIO[FlowRepository, Unit] = ZIO.accessM(_.get.setState(id, state))

  def delete(id: FlowId): RIO[FlowRepository, Unit] = ZIO.accessM(_.get.delete(id))
}
