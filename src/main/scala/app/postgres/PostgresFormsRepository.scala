package app.postgres

import app.Error
import app.forms.{Form, FormId, FormWithId, FormsRepository}
import app.postgres.{Database, MetaInstances}
import doobie.implicits._
import doobie.postgres.implicits._
import zio._
import zio.interop.catz._

private final class PostgresFormsRepository(xa: TaskTransactor) extends FormsRepository with MetaInstances {

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

object PostgresFormsRepository {

  type Env = Has[Database]

  val layer: URLayer[Env, Has[FormsRepository]] =
    Database.getTransactor.map(new PostgresFormsRepository(_)).toLayer

}
