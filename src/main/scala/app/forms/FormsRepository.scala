package app.forms

import zio._

trait FormsRepository {
  def get(id: FormId): Task[Option[FormWithId]]
  def getById(id: FormId): Task[FormWithId]
  def store(form: Form): Task[FormWithId]
}

object FormsRepository {

  def get(id: FormId): RIO[Has[FormsRepository], Option[FormWithId]] =
    ZIO.accessM(_.get.get(id))

  def getById(id: FormId): RIO[Has[FormsRepository], FormWithId] =
    ZIO.accessM(_.get.getById(id))

  def store(form: Form): RIO[Has[FormsRepository], FormWithId] =
    ZIO.accessM(_.get.store(form))
}
