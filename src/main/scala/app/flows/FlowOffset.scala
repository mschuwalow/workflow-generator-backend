package app.flows

final case class FlowOffset(
  flowId: FlowId,
  componentId: ComponentId,
  offset: Long
)
