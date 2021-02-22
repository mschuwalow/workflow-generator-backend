package app.forms

import app.Error
import app.postgres.{Database, MetaInstances}
import doobie.implicits._
import doobie.postgres.implicits._
import zio._
import zio.interop.catz._

object FormsRepository extends MetaInstances {

  trait Service {
    def get(id: FormId): Task[Option[FormWithId]]
    def getById(id: FormId): Task[FormWithId]
    def store(form: Form): Task[FormWithId]
  }

  val doobie: ZLayer[Database, Nothing, FormsRepository] = ZLayer.fromServiceM {
    _.getTransactor.map { xa =>
      new Service {
        def get(id: FormId): Task[Option[FormWithId]] = {
          val query =
            sql"""SELECT form_id, elements, perms
                 |FROM forms
                 |WHERE form_id = $id
                 |""".stripMargin
          query.query[FormWithId].option.transact(xa)
        }

        def getById(id: FormId): Task[FormWithId] =
          get(id).flatMap {
            case Some(flow) => ZIO.succeed(flow)
            case None       => ZIO.fail(Error.NotFound)
          }

        def store(form: Form): Task[FormWithId] = {
          val query =
            sql"""INSERT INTO forms (elements, perms)
                 |VALUES (${form.elements}, ${form.perms})""".stripMargin
          query.update
            .withUniqueGeneratedKeys[FormId]("form_id")
            .map(id => FormWithId(id, form.elements, form.perms))
            .transact(xa)
        }
      }
    }
  }

  def get(id: FormId): RIO[FormsRepository, Option[FormWithId]] =
    ZIO.accessM(_.get.get(id))

  def getById(id: FormId): RIO[FormsRepository, FormWithId] =
    ZIO.accessM(_.get.getById(id))

  def store(form: Form): RIO[FormsRepository, FormWithId] =
    ZIO.accessM(_.get.store(form))
}
