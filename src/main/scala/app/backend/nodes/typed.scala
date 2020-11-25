package app.backend.nodes

import app.backend.Type
import app.backend.Type._

object typed {
  final case class Flow(streams: List[Sink])

  sealed trait Sink {
    def id: ComponentId
  }

  final case class Void(
    id: ComponentId,
    source: Stream)
      extends Sink

  sealed trait Stream {
    def id: ComponentId
    def elementType: Type
  }

  final case class Never(
    id: ComponentId,
    elementType: Type)
      extends Stream

  final case class InnerJoin(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream)
      extends Stream {
    val elementType = tTuple(stream1.elementType, stream2.elementType)
  }

  final case class LeftJoin(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream)
      extends Stream {
    val elementType = tTuple(stream1.elementType, tOption(stream2.elementType))
  }

  final case class Merge(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream)
      extends Stream {
    val elementType = tEither(stream1.elementType, stream2.elementType)
  }

  final case class UDF1(
    id: ComponentId,
    stream: Stream,
    elementType: Type)
      extends Stream

  final case class UDF2(
    id: ComponentId,
    stream1: Stream,
    stream2: Stream,
    elementType: Type)
      extends Stream
}
