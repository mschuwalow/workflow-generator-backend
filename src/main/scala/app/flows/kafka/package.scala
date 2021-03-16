package app.flows

import app.forms.FormId
import zio._

package object kafka {

  def topicForForm(formId: FormId): String =
    s"forms-${formId.value}"

  def topicForFlow(flowId: FlowId, componentId: ComponentId): String =
    s"flow-${flowId.value}-${componentId.value}"
}
