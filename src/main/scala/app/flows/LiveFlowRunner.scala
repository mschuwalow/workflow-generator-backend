package app.flows

import app.flows.StreamsManager.{topicForFlow, topicForForm}
import app.flows.udf.UDFRunner
import app.forms.FormId
import zio._
import zio.logging.{Logger, log}
import zio.stream._

private final class LiveFlowRunner(
  env: LiveFlowRunner.Env
) extends FlowRunner {

  def emitFormOutput(formId: FormId, elementType: Type)(element: elementType.Scala): Task[Unit] = {
    val topicName = topicForForm(formId)
    StreamsManager.publishToStream(topicName, elementType)(Chunk.single(element))
  }.provide(env)

  def run(flow: typed.FlowWithId): Task[Unit] = ???

  def interpretSink(flowId: FlowId, sink: typed.Sink) = {
    import typed.Sink._
    sink match {
      case Void(id, source) =>
        StreamsManager
          .consumeStream(topicForFlow(flowId, source.id), source.elementType, id.value)
          .tap { e =>
            log.info(s"${flowId.value}/${id.value}: Discarded element $e")
          }
          .map(_.commit)
          .runDrain
    }
  }

  def interpretStream(
    flowId: FlowId,
    stream: typed.Stream
  ): ZStream[Has[UDFRunner], Throwable, stream.elementType.Scala] = ???

}

object LiveFlowRunner {

  type Env = Has[UDFRunner] with Has[Logger[String]] with Has[StreamsManager]

  val layer: URLayer[Env, Has[FlowRunner]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveFlowRunner(env)
  }.toLayer

}
