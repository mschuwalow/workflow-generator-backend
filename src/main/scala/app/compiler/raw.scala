package app.compiler

import app.compiler.Type
import io.circe._
import io.circe.generic.semiauto._

object raw {
  final case class Graph(nodes: Map[ComponentId, Component])

  object Graph {

    implicit val decoder: Decoder[Graph] =
      Decoder[Map[ComponentId, Component]].map(Graph.apply)
  }

  sealed trait Component { self =>

    def isSink: Boolean =
      self match {
        case Source.Never(_)                 => false
        case Sink.Void(_, _)                 => true
        case Transformer1.UDF(_, _, _)       => false
        case Transformer2.LeftJoin(_, _)     => false
        case Transformer2.InnerJoin(_, _)    => false
        case Transformer2.Merge(_, _)        => false
        case Transformer2.UDF(_, _, _, _, _) => false
      }
  }

  object Component {

    implicit val decoder: Decoder[Component] =
      Decoder.instance { hCursor =>
        hCursor.get[String]("type").flatMap {
          case "source:never" =>
            hCursor.as[Source.Never]
          case "sink:void" =>
            hCursor.as[Sink.Void]
          case "transformer1:udf" =>
            hCursor.as[Transformer1.UDF]
          case "transformer2:innerJoin" =>
            hCursor.as[Transformer2.InnerJoin]
          case "transformer2:leftJoin" =>
            hCursor.as[Transformer2.LeftJoin]
          case "transformer2:merge" =>
            hCursor.as[Transformer2.Merge]
          case "transformer2:udf" =>
            hCursor.as[Transformer2.UDF]
          case id =>
            Left(
              DecodingFailure(
                s"Not a valid commponent type: ${id}",
                hCursor.history
              )
            )
        }
      }
  }

  sealed trait Source extends Component

  object Source {
    final case class Never(elementType: Option[Type]) extends Source

    object Never {

      implicit val decoder: Decoder[Never] =
        deriveDecoder
    }
  }

  sealed trait Sink extends Component {
    def stream: ComponentId
  }

  object Sink {

    final case class Void(
      stream: ComponentId,
      elementType: Option[Type])
        extends Sink

    object Void {

      implicit val decoder: Decoder[Void] =
        deriveDecoder
    }
  }

  sealed trait Transformer1 extends Component {
    def stream: ComponentId
  }

  object Transformer1 {

    final case class UDF(
      stream: ComponentId,
      inputTypeHint: Option[Type],
      outputTypeHint: Option[Type])
        extends Transformer1

    object UDF {

      implicit val decoder: Decoder[UDF] =
        deriveDecoder
    }
  }

  sealed trait Transformer2 extends Component {
    def stream1: ComponentId
    def stream2: ComponentId
  }

  object Transformer2 {

    final case class LeftJoin(
      stream1: ComponentId,
      stream2: ComponentId)
        extends Transformer2

    object LeftJoin {

      implicit val decoder: Decoder[LeftJoin] =
        deriveDecoder
    }

    final case class InnerJoin(
      stream1: ComponentId,
      stream2: ComponentId)
        extends Transformer2

    object InnerJoin {

      implicit val decoder: Decoder[InnerJoin] =
        deriveDecoder
    }

    final case class Merge(
      stream1: ComponentId,
      stream2: ComponentId)
        extends Transformer2

    object Merge {

      implicit val decoder: Decoder[Merge] =
        deriveDecoder
    }

    final case class UDF(
      stream1: ComponentId,
      stream2: ComponentId,
      input1TypeHint: Option[Type],
      input2TypeHint: Option[Type],
      outputTypeHint: Option[Type])
        extends Transformer2

    object UDF {

      implicit val decoder: Decoder[UDF] =
        deriveDecoder
    }
  }
}
