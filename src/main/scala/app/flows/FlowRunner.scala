package app.flows

import app.forms.FormId
import zio._

trait FlowRunner {
  def run(flow: typed.Flow): Task[Unit]
  def emitFormOutput(formId: FormId, elementType: Type)(element: elementType.Scala): Task[Unit]
}

object FlowRunner {

  def run(flow: typed.Flow): RIO[Has[FlowRunner], Unit] = ZIO.accessM(_.get.run(flow))

  def emitFormOutput(formId: FormId, elementType: Type)(element: elementType.Scala): RIO[Has[FlowRunner], Unit] =
    ZIO.accessM(_.get.emitFormOutput(formId, elementType)(element))

}
