package app.flows

import app.flows.outbound.FlowRepository
import app.forms.inbound.FormsService
import app.jforms.inbound.JFormsService
import zio._

package object inbound {

  val layer: ZLayer[
    LiveFlowRunner.Env
      with Has[FlowRepository]
      with Has[FormsService]
      with Has[
        JFormsService
      ],
    Throwable,
    Has[FlowService]
  ] =
    (ZLayer.identity[
      Has[FlowRepository] with Has[FormsService] with Has[JFormsService]
    ] ++ LiveFlowRunner.layer) >>> LiveFlowService.layer
}
