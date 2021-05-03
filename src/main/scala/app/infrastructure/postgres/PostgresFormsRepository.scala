package app.infrastructure.postgres

import app.Error
import app.forms.outbound.FormsRepository
import app.forms.{CreateFormRequest, Form, FormId}
import doobie.implicits._
import doobie.postgres.implicits._
import zio._
import zio.interop.catz._

private final class PostgresFormsRepository(xa: TaskTransactor) extends FormsRepository with MetaInstances {

  def get(id: FormId): Task[Option[Form]] = {
    val query =
      sql"""SELECT form_id, elements, perms
           |FROM forms
           |WHERE form_id = $id
           |""".stripMargin
    query.query[Form].option.transact(xa)
  }

  def getById(id: FormId): Task[Form] =
    get(id).flatMap {
      case Some(flow) => ZIO.succeed(flow)
      case None       => ZIO.fail(Error.NotFound)
    }

  def create(form: CreateFormRequest): Task[Form] = {
    val query =
      sql"""INSERT INTO forms (elements, perms)
           |VALUES (${form.elements}, ${form.perms})""".stripMargin
    query.update
      .withUniqueGeneratedKeys[FormId]("form_id")
      .map(id => Form(id, form.elements, form.perms))
      .transact(xa)
  }
}

object PostgresFormsRepository {

  type Env = Has[Database]

  val layer: URLayer[Env, Has[FormsRepository]] =
    Database.getTransactor.map(new PostgresFormsRepository(_)).toLayer

}
