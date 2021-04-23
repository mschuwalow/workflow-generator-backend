package app.forms

import zio._

trait FormsService {
  def create(form: CreateFormRequest): Task[Form]
}

object FormsService {

  def create(form: CreateFormRequest): RIO[Has[FormsService], Form] =
    ZIO.accessM(_.get.create(form))
}
