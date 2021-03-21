package app.flows

final case class FlowOffset(
  flowId: FlowId,
  componentId: ComponentId,
  value: Long
)

object FlowOffset {

  def initial(flowId: FlowId, componentId: ComponentId): FlowOffset =
    FlowOffset(flowId, componentId, 0)

}
