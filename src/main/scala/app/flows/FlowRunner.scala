package app.flows

import app.flows.udf.UDFRunner
import app.forms.FormId
import zio._
import zio.logging.{Logger, log}
import zio.stream._

object FlowRunner {

  trait Service {
    def run(flow: typed.FlowWithId): Task[Unit]
    def emitFormOutput(formId: FormId, elementType: Type)(element: elementType.Scala): Task[Unit]
  }

  val stream: ZLayer[UDFRunner with Has[Logger[String]], Nothing, FlowRunner] =
    ZLayer.fromFunctionM { env =>
      Ref.make(Map.empty[FormId, List[internal.Listener]]).map { formsEventListeners =>
        new Service {

          def emitFormOutput(formId: FormId, elementType: Type)(element: elementType.Scala): Task[Unit] =
            formsEventListeners.get.flatMap { listeners =>
              ZIO.foreach_(listeners.getOrElse(formId, Nil))(_.callback(element))
            }

          def run(flow: typed.FlowWithId): Task[Unit] =
            ZIO.foreachPar_(flow.streams)(interpretSink(flow.id, _)).provide(env)

          def interpretSink(flowId: FlowId, sink: typed.Sink) = {
            import typed.Sink._
            sink match {
              case Void(id, source) =>
                interpretStream(flowId, source).mapM(element => log.info(s"$id: Discarded element $element")).runDrain
            }
          }

          def interpretStream(flowId: FlowId, stream: typed.Stream)
            : ZStream[UDFRunner, Throwable, stream.elementType.Scala] = {
            import typed.Stream._
            val anyStream = stream match {
              case InnerJoin(_, stream1, stream2)               =>
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
              case UDF(_, code, stream, elementType)            =>
                interpretStream(flowId, stream).mapM { element =>
                  UDFRunner.runPython(code, stream.elementType, elementType)(element)
                }
              case Numbers(_, values)                           => Stream.fromIterable(values)
              case Never(_, _)                                  =>
                ZStream.never
              case LeftJoin(_, stream1, stream2)                =>
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
              case Merge(_, stream1, stream2)                   =>
                interpretStream(flowId, stream1).mergeEither(interpretStream(flowId, stream2))
              case FormOutput(componentId, formId, elementType) =>
                ZStream.unwrapManaged {
                  for {
                    queue <- Queue.unbounded[elementType.Scala].toManaged(_.shutdown)
                    _     <-
                      ZManaged.make(
                        formsEventListeners.update { old =>
                          old + (formId -> (internal.Listener(
                            flowId,
                            componentId,
                            e => queue.offer(e.asInstanceOf[elementType.Scala]).unit
                          ) :: old.getOrElse(formId, Nil)))
                        }
                      )(_ =>
                        formsEventListeners.update { old =>
                          old + (formId -> (
                            old
                              .getOrElse(formId, Nil)
                              .filterNot(listener => listener.flowId == flowId && listener.componentId == componentId)
                            ))
                        }
                      )
                  } yield ZStream.fromQueue(queue)
                }
            }
            anyStream.asInstanceOf[Stream[Throwable, stream.elementType.Scala]]
          }
        }
      }
    }

  def run(flow: typed.FlowWithId): RIO[FlowRunner, Unit] = ZIO.accessM(_.get.run(flow))

  def emitFormOutput(formId: FormId, elementType: Type)(element: elementType.Scala): RIO[FlowRunner, Unit] =
    ZIO.accessM(_.get.emitFormOutput(formId, elementType)(element))

  private object internal {

    final case class Listener(
      flowId: FlowId,
      componentId: ComponentId,
      callback: Any => UIO[Unit]
    )

  }

}
