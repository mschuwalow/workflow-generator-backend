package app.flows

import app.flows.outbound.FlowRepository
import app.forms.inbound.FormsService
import zio._

package object inbound {

  val layer: ZLayer[LiveFlowRunner.Env with Has[FlowRepository] with Has[FormsService], Throwable, Has[FlowService]] =
    (ZLayer.identity[Has[FlowRepository] with Has[FormsService]] ++ LiveFlowRunner.layer) >>> LiveFlowService.layer
}
