package app.flows

import zio.Task

final class LiveFlowService(
  // streamsManager: StreamsManager
) extends FlowService.Service {

  def add(graph: unresolved.Graph): Task[typed.FlowWithId] = ???

  def check(graph: unresolved.Graph): Task[typed.Flow] = ???

  def delete(id: FlowId): Task[Unit] = ???

}
