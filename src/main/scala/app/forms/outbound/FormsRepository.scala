package app.forms.outbound

import app.forms._

import zio._

trait FormsRepository {
  def get(id: FormId): Task[Option[Form]]
  def getById(id: FormId): Task[Form]
  def create(form: CreateFormRequest): Task[Form]
}

object FormsRepository {

  def get(id: FormId): RIO[Has[FormsRepository], Option[Form]] =
    ZIO.accessM(_.get.get(id))

  def getById(id: FormId): RIO[Has[FormsRepository], Form] =
    ZIO.accessM(_.get.getById(id))

  def create(form: CreateFormRequest): RIO[Has[FormsRepository], Form] =
    ZIO.accessM(_.get.create(form))
}
