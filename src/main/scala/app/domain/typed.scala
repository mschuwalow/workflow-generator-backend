package app.domain

import app.UDFRunner
import app.domain.Type._
import doobie.util.meta.Meta
import io.circe._
import io.circe.generic.semiauto._
import io.circe.jawn._
import io.circe.syntax._
import zio.stream.ZStream
import zio.{Has, ZIO}

object typed {
  final case class Flow(streams: List[Sink])

  object Flow {

    implicit val encoder: Encoder[Flow] =
      deriveEncoder

    implicit val decoder: Decoder[Flow] =
      deriveDecoder

    implicit val meta: Meta[Flow] =
      Meta[String].timap(parse(_).flatMap(_.as[Flow]).toOption.get)(
        _.asJson.noSpaces
      )
  }

  final case class FlowWithId(id: FlowId, flow: Flow, state: FlowState)

  object FlowWithId {

    implicit val encoder: Encoder[FlowWithId] =
      deriveEncoder

    implicit val decoder: Decoder[FlowWithId] =
      deriveDecoder
  }

  sealed trait Sink {
    def id: ComponentId
    def run: ZIO[UDFRunner, Throwable, Unit]
  }

  object Sink {

    implicit val encoder: Encoder[Sink] =
      deriveEncoder

    implicit val decoder: Decoder[Sink] =
      deriveDecoder
  }

  final case class Void(id: ComponentId, source: Stream) extends Sink {
    val run = source.run.runDrain
  }

  sealed trait Stream {
    type Element
    val id: ComponentId
    val elementType: Type
    val run: ZStream[UDFRunner, Throwable, Element]

    def asElementType(elem: Element): elementType.Scala =
      elem.asInstanceOf[elementType.Scala]
  }

  object Stream {

    implicit val encoder: Encoder[Stream] =
      deriveEncoder

    implicit val decoder: Decoder[Stream] =
      deriveDecoder
  }

  final case class Never(id: ComponentId, elementType: Type) extends Stream {
    type Element = Nothing
    val run: ZStream[Any, Throwable, Element] = ZStream.never
  }

  final case class Numbers(id: ComponentId, values: List[Long]) extends Stream {
    type Element = Long
    val elementType: Type = Type.tNumber

    val run: ZStream[Has[UDFRunner.Service], Throwable, Long] =
      ZStream.fromIterable(values)
  }

  final case class InnerJoin(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
    type Element = (stream1.Element, stream2.Element)
    val elementType = TTuple(stream1.elementType, stream2.elementType)

    val run: ZStream[UDFRunner, Throwable, Element] =
      stream1.run
        .mergeEither(stream2.run)
        .mapAccum(
          (None: Option[stream1.Element], None: Option[stream2.Element])
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
  }

  final case class LeftJoin(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
    type Element = (stream1.Element, Option[stream2.Element])
    val elementType = tTuple(stream1.elementType, tOption(stream2.elementType))

    val run: ZStream[UDFRunner, Throwable, Element] =
      stream1.run
        .mergeEither(stream2.run)
        .mapAccum(
          (None: Option[stream1.Element], None: Option[stream2.Element])
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
  }

  final case class Merge(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
    type Element = Either[stream1.Element, stream2.Element]
    val elementType: TEither = TEither(stream1.elementType, stream2.elementType)

    val run: ZStream[UDFRunner, Throwable, Element] =
      stream1.run.mergeEither(stream2.run)
  }

  final case class UDF(id: ComponentId, code: String, stream: Stream, elementType: Type) extends Stream {
    type Element = elementType.Scala

    val run: ZStream[UDFRunner, Throwable, Element] =
      stream.run.mapM { element =>
        UDFRunner.runPython1(code, stream.elementType, elementType)(
          stream.asElementType(element)
        )
      }
  }

}
