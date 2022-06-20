package app.flows

import app.Type._
import app.forms.FormId
import app.jforms.JFormId
import app.{Getter, Type}
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto._

import scala.annotation.unused

object typed {
  final case class Flow(id: FlowId, sinks: List[Sink], state: FlowState)

  object Flow {

    implicit val encoder: Encoder[Flow] =
      deriveEncoder

    implicit val decoder: Decoder[Flow] =
      deriveDecoder
  }

  final case class CreateFlowRequest(streams: List[Sink])

  object CreateFlowRequest {

    implicit val encoder: Encoder[CreateFlowRequest] =
      deriveEncoder

    implicit val decoder: Decoder[CreateFlowRequest] =
      deriveDecoder
  }

  sealed trait Sink {
    def id: ComponentId
    val stream: Stream
  }

  object Sink {

    final case class Void(id: ComponentId, stream: Stream) extends Sink

    @unused
    implicit val configuration: Configuration =
      Configuration.default.withDiscriminator("type")

    implicit val encoder: Encoder[Sink] =
      deriveConfiguredEncoder

    implicit val decoder: Decoder[Sink] =
      deriveConfiguredDecoder
  }

  sealed trait Stream {
    val id: ComponentId
    val elementType: Type
    val sources: Set[Stream.Source]
  }

  object Stream {

    sealed trait Source extends Stream { self =>
      final val sources: Set[Source] = Set(self)
    }

    final case class FormOutput(id: ComponentId, formId: FormId, elementType: Type) extends Source

    final case class JFormOutput(id: ComponentId, formId: JFormId, elementType: Type) extends Source

    final case class Never(id: ComponentId, elementType: Type) extends Source

    final case class Numbers(id: ComponentId, values: List[Long]) extends Source {
      val elementType = Type.TNumber
    }

    final case class Zip(
      id: ComponentId,
      leftStream: Stream,
      rightStream: Stream,
      onLeftField: Getter,
      onRightField: Getter
    ) extends Stream {
      val elementType = TTuple(leftStream.elementType, rightStream.elementType)
      val sources     = leftStream.sources ++ rightStream.sources
    }

    final case class InnerJoin(
      id: ComponentId,
      leftStream: Stream,
      rightStream: Stream,
      onLeftField: Getter,
      onRightField: Getter
    ) extends Stream {
      val elementType = TTuple(leftStream.elementType, rightStream.elementType)
      val sources     = leftStream.sources ++ rightStream.sources
    }

    final case class LeftJoin(
      id: ComponentId,
      leftStream: Stream,
      rightStream: Stream,
      onLeftField: Getter,
      onRightField: Getter
    ) extends Stream {
      val elementType = TTuple(leftStream.elementType, TOption(rightStream.elementType))
      val sources     = leftStream.sources ++ rightStream.sources
    }

    final case class Merge(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
      assert(stream1.elementType == stream2.elementType)

      val elementType = stream1.elementType
      val sources     = stream1.sources ++ stream2.sources
    }

    final case class MergeEither(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
      val elementType = TEither(stream1.elementType, stream2.elementType)
      val sources     = stream1.sources ++ stream2.sources
    }

    final case class UDF(id: ComponentId, code: String, stream: Stream, elementType: Type) extends Stream {
      val sources = stream.sources
    }

    @unused
    implicit val configuration: Configuration =
      Configuration.default.withDiscriminator("type")

    implicit val encoder: Encoder[Stream] =
      deriveConfiguredEncoder

    implicit val decoder: Decoder[Stream] =
      deriveConfiguredDecoder
  }
}
