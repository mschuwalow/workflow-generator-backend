package app.flows.inbound

import app.flows._
import app.flows.outbound._
import app.forms.inbound.FormsService
import app.jforms.inbound.JFormsService
import app.utils.StreamSyntax
import zio._
import zio.logging.{Logger, log}
import zio.stream._

private final class LiveFlowRunner(
  env: LiveFlowRunner.Env
) extends FlowRunner
    with StreamSyntax {

  type SourcesStreamMap = Map[ComponentId, ZStream[Any, Nothing, Any]]

  def run(flow: typed.Flow): Task[Unit] =
    Promise
      .makeManaged[Nothing, Unit]
      .flatMap { startConsuming =>
        for {
          sources <- collectSources(flow, startConsuming)
          fibers  <- ZManaged.foreach(flow.sinks) { s =>
                       Promise.makeManaged[Nothing, Unit].flatMap { started =>
                         runStream(flow.id, s, sources, started.succeed(()).unit).toManaged_.fork.map((_, started))
                       }
                     }
          _       <- ZIO.foreach(fibers)(_._2.await).toManaged_
          _       <- startConsuming.succeed(()).toManaged_
          _       <- ZIO.foreach(fibers)(_._1.join).toManaged_
        } yield ()
      }
      .useNow
      .provide(env)

  private def collectSources(
    flow: typed.Flow,
    promise: Promise[Nothing, Unit]
  ): Managed[Nothing, SourcesStreamMap] = {
    import typed.Stream._
    ZManaged.foldLeft(flow.sinks.flatMap(_.stream.sources))(Map.empty: SourcesStreamMap) { case (acc, s) =>
      s match {
        case FormOutput(id, formId, elementType)  =>
          Stream
            .fromEffect(promise.await)
            .crossRight(
              ZStream
                .fromEffect(FormsService.getById(formId))
                .flatMap(form =>
                  if (form.outputType == elementType) {
                    FormsService.subscribe(form)
                  } else {
                    ZStream.dieMessage(s"Form type mismatch. expected: $elementType; got: ${form.outputType}")
                  }
                )
            )
            .broadcastDynamic(256)
            .map(s => acc + (id -> s))
            .provide(env)
        case JFormOutput(id, formId, elementType) =>
          Stream
            .fromEffect(promise.await)
            .crossRight(
              ZStream
                .fromEffect(JFormsService.getById(formId))
                .flatMap(form =>
                  if (form.outputType == elementType) {
                    JFormsService.subscribe(form)
                  } else {
                    ZStream.dieMessage(s"Form type mismatch. expected: $elementType; got: ${form.outputType}")
                  }
                )
            )
            .broadcastDynamic(256)
            .map(s => acc + (id -> s))
            .provide(env)
        case Never(id, _)                         =>
          ZManaged.succeed(acc + (id -> ZStream.never))
        case Numbers(id, values)                  =>
          ZManaged.succeed(acc + (id -> ZStream.fromIterable(values)))
      }
    }
  }

  private def runStream(
    flowId: FlowId,
    sink: typed.Sink,
    sources: SourcesStreamMap,
    onStart: UIO[Unit]
  ) =
    interpretSink(flowId, sink).use { push =>
      FlowOffsetRepository
        .get(flowId, sink.id)
        .flatMap { offset =>
          interpretStream(sink.stream, sources)
            .onFirstPull(onStart)
            .zipWithIndex
            .foldM(offset) { case (offset, (e, i)) =>
              if (offset.value > i)
                ZIO.succeed(offset)
              else
                for {
                  _         <- push(e)
                  nextOffset = offset.copy(value = offset.value + 1)
                  _         <- FlowOffsetRepository.put(nextOffset)
                } yield nextOffset
            }
        }
    }

  private def interpretSink(
    flowId: FlowId,
    sink: typed.Sink
  ): Managed[Nothing, sink.stream.elementType.Scala => UIO[Unit]] = {
    import typed.Sink._
    sink match {
      case Void(id, _) =>
        ZManaged.succeed { e =>
          log
            .info(s"${flowId.value}/${id.value}: Discarded element $e")
            .provide(env)
        }
    }
  }

  private def interpretStream(
    stream: typed.Stream,
    sources: SourcesStreamMap
  ): ZStream[Has[UDFRunner] with Has[FormsService], Throwable, stream.elementType.Scala] = {
    import typed.Stream._
    def go(
      stream: typed.Stream
    ): ZStream[Has[UDFRunner] with Has[FormsService], Throwable, stream.elementType.Scala] = {
      def consumeSource(id: ComponentId) =
        ZStream.fromIterable(sources.get(id)).flatten

      val anyStream: ZStream[Has[UDFRunner] with Has[FormsService], Throwable, Any] = stream match {
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
            .collect { case (Some(l), Some(r)) =>
              ((l, r))
            }
        case UDF(_, code, stream, elementType) =>
          go(stream).mapM { element =>
            UDFRunner.runPython(code, stream.elementType, elementType)(element)
          }
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
            .collect { case (Some(l), r) =>
              ((l, r))
            }
        case Merge(_, stream1, stream2)        =>
          go(stream1).merge(go(stream2))
        case MergeEither(_, stream1, stream2)  =>
          go(stream1).mergeEither(go(stream2))
        case source: typed.Stream.Source       =>
          consumeSource(source.id)
      }
      anyStream.asInstanceOf[ZStream[Has[UDFRunner] with Has[FormsService], Throwable, stream.elementType.Scala]]
    }
    go(stream)
  }
}

private[inbound] object LiveFlowRunner {

  type Env = Has[UDFRunner]
    with Has[Logger[String]]
    with Has[FormsService]
    with Has[FlowOffsetRepository]
    with Has[JFormsService]

  val layer: URLayer[Env, Has[FlowRunner]] = {
    for {
      env <- ZIO.environment[Env]
    } yield new LiveFlowRunner(env)
  }.toLayer

}
