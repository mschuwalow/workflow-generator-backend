package app.compiler

import app.compiler.Type._
import zio.ZIO
import zio.stream.ZStream

object typed {
  final case class Flow(streams: List[Sink])

  sealed trait Sink {
    def id: ComponentId
    def run: ZIO[Any, Throwable, Unit]
  }

  final case class Void(
    id: ComponentId,
    source: Stream)
      extends Sink {
    val run = source.run.runDrain
  }

  sealed trait Stream {
    type Element
    val id: ComponentId
    val elementType: Type
    val run: ZStream[Any, Throwable, Element]
  }

  final case class Never(
    id: ComponentId,
    elementType: Type)
      extends Stream {
    type Element = Nothing
    val run: ZStream[Any, Throwable, Element] = ZStream.never
  }

  final case class InnerJoin(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream)
      extends Stream {
    type Element = (stream1.Element, stream2.Element)
    val elementType = TTuple(stream1.elementType, stream2.elementType)

    val run: ZStream[Any, Throwable, Element] = {
      stream1.run
        .mergeEither(stream2.run)
        .mapAccum(
          (None: Option[stream1.Element], None: Option[stream2.Element])
        ) {
          case ((_, r), Left(l)) =>
            val result = (Some(l), r)
            (result, result)
          case ((l, _), Right(r)) =>
            val result = (l, Some(r))
            (result, result)
        }
        .collect {
          case ((Some(l), Some(r))) => ((l, r))
        }
    }
  }

  final case class LeftJoin(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream)
      extends Stream {
    type Element = (stream1.Element, Option[stream2.Element])
    val elementType = tTuple(stream1.elementType, tOption(stream2.elementType))

    val run: ZStream[Any, Throwable, Element] = {
      stream1.run
        .mergeEither(stream2.run)
        .mapAccum(
          (None: Option[stream1.Element], None: Option[stream2.Element])
        ) {
          case ((_, r), Left(l)) =>
            val result = (Some(l), r)
            (result, result)
          case ((l, _), Right(r)) =>
            val result = (l, Some(r))
            (result, result)
        }
        .collect {
          case ((Some(l), r)) => ((l, r))
        }
    }
  }

  final case class Merge(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream)
      extends Stream {
    type Element = Either[stream1.Element, stream2.Element]
    val elementType: TEither = TEither(stream1.elementType, stream2.elementType)

    val run: ZStream[Any, Throwable, Element] =
      stream1.run.mergeEither(stream2.run)
  }

  final case class UDF1(
    id: ComponentId,
    stream: Stream,
    elementType: Type)
      extends Stream {
    type Element = elementType.Scala
    val run: ZStream[Any, Throwable, Element] = ???
  }

  final case class UDF2(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream,
    elementType: Type)
      extends Stream {
    type Element = elementType.Scala
    val run: ZStream[Any, Throwable, Element] = ???
  }
}
