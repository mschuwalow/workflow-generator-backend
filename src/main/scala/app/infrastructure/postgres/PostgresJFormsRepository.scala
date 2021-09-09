package app.infrastructure.postgres

import app.Error
import app.jforms._
import app.jforms.outbound.JFormsRepository
import doobie.implicits._
import doobie.postgres.circe.jsonb.implicits._
import doobie.postgres.implicits._
import zio._
import zio.interop.catz._

private final class PostgresJFormsRepository(xa: TaskTransactor) extends JFormsRepository with MetaInstances {

  def get(id: JFormId): Task[Option[JForm]] = {
    val query =
      sql"""SELECT jform_id, data_schema, ui_schema, perms
           |FROM jforms
           |WHERE jform_id = $id
           |""".stripMargin
    query.query[JForm].option.transact(xa)
  }

  def getOrFail(id: JFormId): Task[JForm] =
    get(id).flatMap {
      case Some(form) => ZIO.succeed(form)
      case None       => ZIO.fail(Error.NotFound)
    }

  def create(form: CreateJFormRequest): Task[JForm] = {
    val query =
      sql"""INSERT INTO jforms (data_schema, ui_schema, perms)
           |VALUES (${form.dataSchema}, ${form.uiSchema}, ${form.perms})""".stripMargin
    query.update
      .withUniqueGeneratedKeys[JFormId]("jform_id")
      .map(id => JForm(id, form.dataSchema, form.uiSchema, form.perms))
      .transact(xa)
  }
}

object PostgresJFormsRepository {

  type Env = Has[Database]

  val layer: URLayer[Env, Has[JFormsRepository]] =
    Database.getTransactor.map(new PostgresJFormsRepository(_)).toLayer

}
