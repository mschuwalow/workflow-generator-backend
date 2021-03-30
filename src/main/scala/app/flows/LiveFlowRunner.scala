package app.flows

import app.flows.StreamsManager.{topicForFlow, topicForForm}
import app.flows.udf.UDFRunner
import app.forms.FormId
import app.utils.StreamSyntax
import zio._
import zio.logging.{Logger, log}
import zio.stream._

private final class LiveFlowRunner(
  env: LiveFlowRunner.Env
) extends FlowRunner
    with StreamSyntax {

  type SourcesStreamMap = Map[ComponentId, UIO[ZStream[Any, Nothing, Any]]]

  def emitFormOutput(formId: FormId, elementType: Type)(element: elementType.Scala): Task[Unit] = {
    val topicName = topicForForm(formId)
    StreamsManager.publishToStream(topicName, elementType)(Chunk.single(element))
  }.provide(env)

  def run(flow: typed.FlowWithId): Task[Unit] = {
    val terminal = flow.streams.map(_.source).distinct
    Promise
      .makeManaged[Nothing, Unit]
      .flatMap { startConsuming =>
        val sourcesM = ZManaged.foldLeft(terminal)(Map.empty: SourcesStreamMap) {
          case (acc, s) =>
            collectSources(s, flow.id, startConsuming, acc)
        }
        for {
          sources <- sourcesM
          started <- ZManaged.foreach(terminal) { t =>
                       Promise.makeManaged[Nothing, Unit].flatMap { started =>
                         runTerminal(flow.id, t, sources, started.succeed(()).unit).toManaged_.fork.as(started)
                       }
                     }
          _       <- ZIO.foreach(started)(_.await).toManaged_
          _       <- startConsuming.succeed(()).toManaged_
          f2      <- ZManaged.foreach(flow.streams)(runSink(flow.id, _).toManaged_.fork)
          _       <- ZIO.foreach(f2)(_.join).toManaged_
        } yield ()
      }
      .useNow
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

  def collectSources(
    stream: typed.Stream,
    flowId: FlowId,
    promise: Promise[Nothing, Unit],
    sources: SourcesStreamMap
  ): Managed[Nothing, SourcesStreamMap] = {
    import typed.Stream._
    def go(stream: typed.Stream, acc: SourcesStreamMap): Managed[Nothing, SourcesStreamMap] =
      stream match {
        case InnerJoin(_, stream1, stream2)      =>
          go(stream1, acc).flatMap(go(stream2, _))
        case UDF(_, _, stream, _)                =>
          go(stream, acc)
        case Numbers(_, _)                       =>
          ZManaged.succeed(acc)
        case Never(_, _)                         =>
          ZManaged.succeed(acc)
        case LeftJoin(_, stream1, stream2)       =>
          go(stream1, acc).flatMap(go(stream2, _))
        case Merge(_, stream1, stream2)          =>
          go(stream1, acc).flatMap(go(stream2, _))
        case FormOutput(id, formId, elementType) =>
          if (acc.contains(id))
            ZManaged.succeed(acc)
          else
            Stream
              .fromEffect(promise.await)
              .crossRight(
                StreamsManager
                  .consumeStream(topicForForm(formId), elementType, Some(s"${flowId.value}-${id.value}"))
              )
              .map(_.value)
              .broadcastDynamic(32)
              .map { s =>
                acc + (id -> s)
              }
              .provide(env)
      }

    go(stream, sources)
  }

  def runTerminal(
    flowId: FlowId,
    stream: typed.Stream,
    sources: SourcesStreamMap,
    onStart: UIO[Unit]
  ) =
    FlowOffsetRepository
      .get(flowId, stream.id)
      .map(_.getOrElse(FlowOffset.initial(flowId, stream.id)))
      .flatMap { offset =>
        val streamName = topicForFlow(flowId, stream.id)
        interpretStream(stream, sources)
          .onFirstPull(onStart)
          .zipWithIndex
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
    stream: typed.Stream,
    sources: SourcesStreamMap
  ): ZStream[Has[UDFRunner] with Has[StreamsManager], Throwable, stream.elementType.Scala] = {
    import typed.Stream._
    def go(
      stream: typed.Stream
    ): ZStream[Has[UDFRunner] with Has[StreamsManager], Throwable, stream.elementType.Scala] = {
      val anyStream: ZStream[Has[UDFRunner] with Has[StreamsManager], Throwable, Any] = stream match {
        case InnerJoin(_, stream1, stream2)    =>
          go(stream1)
            .mergeEither(go(stream2))
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
        case UDF(_, code, stream, elementType) =>
          go(stream).mapM { element =>
            UDFRunner.runPython(code, stream.elementType, elementType)(element)
          }
        case Numbers(_, values)                =>
          Stream.fromIterable(values)
        case Never(_, _)                       =>
          ZStream.never
        case LeftJoin(_, stream1, stream2)     =>
          go(stream1)
            .mergeEither(go(stream2))
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
        case Merge(_, stream1, stream2)        =>
          go(stream1).mergeEither(go(stream2))
        case FormOutput(id, _, _)              =>
          ZStream.fromEffect(sources(id))
      }
      anyStream.asInstanceOf[ZStream[Has[UDFRunner] with Has[StreamsManager], Throwable, stream.elementType.Scala]]
    }
    go(stream)
  }

  def consumeSinkStream[R, E, A](flowId: FlowId, sink: typed.Sink)(f: sink.source.elementType.Scala => ZIO[R, E, A]) = {
    val source: sink.source.type = sink.source
    StreamsManager
      .consumeStream(topicForFlow(flowId, source.id), source.elementType, Some(sink.id.value))
      .tap(e => f(e.value))
      .tap(_.commit)
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
