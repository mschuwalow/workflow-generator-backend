package app.flows

import app.flows.Type._
import app.forms.FormId
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto._

import scala.annotation.unused

object typed {
  final case class Flow(streams: List[Sink])

  object Flow {

    implicit val encoder: Encoder[Flow] =
      deriveEncoder

    implicit val decoder: Decoder[Flow] =
      deriveDecoder
  }

  final case class FlowWithId(id: FlowId, streams: List[Sink], state: FlowState)

  object FlowWithId {

    implicit val encoder: Encoder[FlowWithId] =
      deriveEncoder

    implicit val decoder: Decoder[FlowWithId] =
      deriveDecoder
  }

  sealed trait Sink {
    def id: ComponentId
    val source: Stream
  }

  object Sink {

    final case class Void(id: ComponentId, source: Stream) extends Sink

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
  }

  object Stream {

    final case class FormOutput(id: ComponentId, formId: FormId, elementType: Type) extends Stream

    final case class Never(id: ComponentId, elementType: Type) extends Stream

    final case class Numbers(id: ComponentId, values: List[Long]) extends Stream {
      val elementType = Type.TNumber
    }

    final case class InnerJoin(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
      val elementType = TTuple(stream1.elementType, stream2.elementType)
    }

    final case class LeftJoin(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
      val elementType = TTuple(stream1.elementType, TOption(stream2.elementType))
    }

    final case class Merge(id: ComponentId, stream1: Stream, stream2: Stream) extends Stream {
      val elementType = TEither(stream1.elementType, stream2.elementType)
    }

    final case class UDF(id: ComponentId, code: String, stream: Stream, elementType: Type) extends Stream

    @unused
    implicit val configuration: Configuration =
      Configuration.default.withDiscriminator("type")

    implicit val encoder: Encoder[Stream] =
      deriveConfiguredEncoder

    implicit val decoder: Decoder[Stream] =
      deriveConfiguredDecoder
  }
}
