package app.flows

import app.flows._
import zio._

trait FlowOffsetRepository {
  def put(offset: FlowOffset): UIO[Unit]
  def get(flowId: FlowId, componentId: ComponentId): UIO[Option[FlowOffset]]
}

object FlowOffsetRepository {

  def put(offset: FlowOffset): URIO[Has[FlowOffsetRepository], Unit] =
    ZIO.accessM(_.get.put(offset))

  def get(flowId: FlowId, componentId: ComponentId): URIO[Has[FlowOffsetRepository], Option[FlowOffset]] =
    ZIO.accessM(_.get.get(flowId, componentId))

}
