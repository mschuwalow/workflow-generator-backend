package app.jforms.outbound

import app.jforms._
import zio._

trait JFormsRepository {
  def get(id: JFormId): Task[Option[JForm]]
  def getOrFail(id: JFormId): Task[JForm]
  def create(form: CreateJFormRequest): Task[JForm]
}

object JFormsRepository {

  def get(id: JFormId): RIO[Has[JFormsRepository], Option[JForm]] =
    ZIO.accessM(_.get.get(id))

  def getById(id: JFormId): RIO[Has[JFormsRepository], JForm] =
    ZIO.accessM(_.get.getOrFail(id))

  def create(form: CreateJFormRequest): RIO[Has[JFormsRepository], JForm] =
    ZIO.accessM(_.get.create(form))
}
