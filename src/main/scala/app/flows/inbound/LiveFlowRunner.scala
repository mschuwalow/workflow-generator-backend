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

  def run(flow: typed.Flow): Task[Unit] = {
    for {
      sources <- collectSources(flow)
      _       <-
        ZIO.foreachPar_(flow.sinks)(runStream(flow.id, _, sources)).tapError(e => UIO(println(s"Error: $e"))).toManaged_
    } yield ()
  }.useNow.provide(env)

  private def collectSources(
    flow: typed.Flow
  ): Managed[Nothing, SourcesStreamMap] = {
    import typed.Stream._
    ZManaged.foldLeft(flow.sinks.flatMap(_.stream.sources))(Map.empty: SourcesStreamMap) { case (acc, s) =>
      s match {
        case FormOutput(id, formId, elementType)  =>
          ZManaged
            .fromEffect(FormsService.getById(formId))
            .mapM { form =>
              if (form.outputType == elementType) {
                ZIO.succeed(FormsService.subscribe(form).provide(env))
              } else {
                ZIO.dieMessage(s"Form type mismatch. expected: $elementType; got: ${form.outputType}")
              }
            }
            .map(s => acc + (id -> s))
            .provide(env)
        case JFormOutput(id, formId, elementType) =>
          ZManaged
            .fromEffect(JFormsService.getById(formId))
            .mapM { form =>
              if (form.outputType == elementType) {
                ZIO.succeed(JFormsService.subscribe(form).provide(env))
              } else {
                ZIO.dieMessage(s"Form type mismatch. expected: $elementType; got: ${form.outputType}")
              }
            }
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
    sources: SourcesStreamMap
  ) =
    interpretSink(flowId, sink).use { push =>
      FlowOffsetRepository
        .get(flowId, sink.id)
        .flatMap { offset =>
          interpretStream(sink.stream, sources).zipWithIndex
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
        case UDF(_, code, stream, elementType) =>
          go(stream).mapM(UDFRunner.runPython(code, stream.elementType, elementType))

        case Zip(_, s1, s2, f1, f2)            =>
          go(s1).groupByKey(s1.elementType.get(f1, _)) {
            case (None, failed) =>
              failed.tap(e => log.warn(s"Could not extract element using ${f1.show} from $e")).drain.provide(env)
            case (Some(k), z1)  =>
              val z2 = go(s2)
                .map(e => s2.elementType.get(f2, e).map((_, e)).toList)
                .flattenIterables
                .filter(_._1 == k)
                .map(_._2)
              z1.zip(z2)
          }

        case InnerJoin(_, s1, s2, f1, f2) =>
          go(s1).groupByKey(s1.elementType.get(f1, _)) {
            case (None, failed) =>
              failed.tap(e => log.warn(s"Could not extract element using ${f1.show} from $e")).drain.provide(env)
            case (Some(k), z1)  =>
              val z2 =
                go(s2)
                .map(e => s2.elementType.get(f2, e).map((_, e)).toList)
                .flattenIterables
                .filter(_._1 == k)
                .map(_._2)

              z1.flattenParUnbounded { v =>
                z2.map((z1, _))
              }

              z1.mergeEither {
                go(s2)
                  .map(e => s2.elementType.get(f2, e).map((_, e)).toList)
                  .flattenIterables
                  .filter(_._1 == k)
                  .map(_._2)
              }
                .mapAccum(
                  (None: Option[s1.elementType.Scala], None: Option[s2.elementType.Scala])
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
          }

        case LeftJoin(_, s1, s2, f1, f2)      =>
          go(s1).groupByKey(s1.elementType.get(f1, _)) {
            case (None, failed) =>
              failed.tap(e => log.warn(s"Could not extract element using ${f1.show} from $e")).drain.provide(env)
            case (Some(k), z1)  =>
              z1.mergeEither {
                go(s2)
                  .map(e => s2.elementType.get(f2, e).map((_, e)).toList)
                  .flattenIterables
                  .filter(_._1 == k)
                  .map(_._2)
              }
                .mapAccum(
                  (None: Option[s1.elementType.Scala], None: Option[s2.elementType.Scala])
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
          }
        case Merge(_, stream1, stream2)       =>
          go(stream1).merge(go(stream2))
        case MergeEither(_, stream1, stream2) =>
          go(stream1).mergeEither(go(stream2))
        case source: typed.Stream.Source      =>
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
