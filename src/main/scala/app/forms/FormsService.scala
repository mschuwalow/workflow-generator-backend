package app.forms

import zio._

trait FormsService {
  def create(form: Form): Task[FormWithId]
}

object FormsService {

  def create(form: Form): RIO[Has[FormsService], FormWithId] =
    ZIO.accessM(_.get.create(form))
}
