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

  def run(flow: typed.FlowWithId): Task[Unit] = {
    val terminal = flow.streams.map(_.source).distinct
    for {
      f1 <- ZIO.forkAll(terminal.map(runTerminal(flow.id, _)))
      f2 <- ZIO.forkAll(flow.streams.map(runSink(flow.id, _)))
      _  <- f1.join *> f2.join // autointerruption will take care if we get interrupted
    } yield ()
  }.provide(env)

  def runSink(flowId: FlowId, sink: typed.Sink) = {
    import typed.Sink._
    consumeSinkStream(flowId, sink) { e =>
      sink match {
        case Void(id, _) =>
          log.info(s"${flowId.value}/${id.value}: Discarded element $e")
      }
    }
  }

  def runTerminal(
    flowId: FlowId,
    stream: typed.Stream
  ) =
    FlowOffsetRepository
      .get(flowId, stream.id)
      .map(_.getOrElse(FlowOffset.initial(flowId, stream.id)))
      .flatMap { offset =>
        val streamName = topicForFlow(flowId, stream.id)

        interpretStream(flowId, stream).zipWithIndex
          .foldM(offset) {
            case (offset, (e, i)) =>
              if (offset.value > i)
                ZIO.succeed(offset)
              else
                for {
                  _         <- StreamsManager.publishToStream(streamName, stream.elementType)(Chunk.single(e))
                  nextOffset = offset.copy(value = offset.value + 1)
                  _         <- FlowOffsetRepository.put(nextOffset)
                } yield nextOffset
          }
          .unit
      }

  def interpretStream(
    flowId: FlowId,
    stream: typed.Stream
  ): ZStream[Has[UDFRunner] with Has[StreamsManager], Throwable, stream.elementType.Scala] = {
    import typed.Stream._
    val anyStream: ZStream[Has[UDFRunner] with Has[StreamsManager], Throwable, Any] = stream match {
      case InnerJoin(_, stream1, stream2)     =>
        interpretStream(flowId, stream1)
          .mergeEither(interpretStream(flowId, stream2))
          .mapAccum(
            (None: Option[stream1.elementType.Scala], None: Option[stream2.elementType.Scala])
          ) {
            case ((_, r), Left(l))  =>
              val result = (Some(l), r)
              (result, result)
            case ((l, _), Right(r)) =>
              val result = (l, Some(r))
              (result, result)
          }
          .collect {
            case (Some(l), Some(r)) =>
              ((l, r))
          }
      case UDF(_, code, stream, elementType)  =>
        interpretStream(flowId, stream).mapM { element =>
          UDFRunner.runPython(code, stream.elementType, elementType)(element)
        }
      case Numbers(_, values)                 =>
        Stream.fromIterable(values)
      case Never(_, _)                        =>
        ZStream.never
      case LeftJoin(_, stream1, stream2)      =>
        interpretStream(flowId, stream1)
          .mergeEither(interpretStream(flowId, stream2))
          .mapAccum(
            (None: Option[stream1.elementType.Scala], None: Option[stream2.elementType.Scala])
          ) {
            case ((_, r), Left(l))  =>
              val result = (Some(l), r)
              (result, result)
            case ((l, _), Right(r)) =>
              val result = (l, Some(r))
              (result, result)
          }
          .collect {
            case (Some(l), r) =>
              ((l, r))
          }
      case Merge(_, stream1, stream2)         =>
        interpretStream(flowId, stream1).mergeEither(interpretStream(flowId, stream2))
      case FormOutput(id, formId, elementType) =>
        StreamsManager
          .consumeStream(topicForForm(formId), elementType, Some(s"${flowId.value}-${id.value}"))
          .map(_.value)
    }
    anyStream.asInstanceOf[Stream[Throwable, stream.elementType.Scala]]
  }

  def consumeSinkStream[R, E, A](flowId: FlowId, sink: typed.Sink)(f: sink.source.elementType.Scala => ZIO[R, E, A]) = {
    val source: sink.source.type = sink.source
    StreamsManager
      .consumeStream(topicForFlow(flowId, source.id), source.elementType, Some(sink.id.value))
      .tap(e => f(e.value))
      .map(_.commit)
      .runDrain
  }

}

object LiveFlowRunner {

  type Env = Has[UDFRunner] with Has[Logger[String]] with Has[StreamsManager] with Has[FlowOffsetRepository]

  val layer: URLayer[Env, Has[FlowRunner]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveFlowRunner(env)
  }.toLayer

}
