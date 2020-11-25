package app.backend.nodes

import app.backend.Type
import io.circe._

object raw {
  final case class Graph(nodes: Map[ComponentId, Component])

  sealed trait Component

  object Component {

    implicit val decoder: Decoder[Component] =
      Decoder.instance { hCursor =>
        hCursor.get[ComponentId]("type").flatMap {
          case id =>
            Left(
              DecodingFailure(
                s"Not a valid commponent type: ${id.value}",
                hCursor.history
              )
            )
        }
      }
  }

  sealed trait Source extends Component

  object Source {
    final case class Never(elementType: Type) extends Source
  }

  sealed trait Sink extends Component {
    def stream: ComponentId
  }

  object Sink {

    final case class Void(
      elementType: Type,
      stream: ComponentId)
        extends Sink
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

    final case class InnerJoin(
      stream1: ComponentId,
      stream2: ComponentId)
        extends Transformer2

    final case class Merge(
      stream1: ComponentId,
      stream2: ComponentId)
        extends Transformer2

    final case class UDF(
      stream1: ComponentId,
      stream2: ComponentId,
      input1TypeHint: Option[Type],
      input2TypeHint: Option[Type],
      outputTypeHint: Option[Type])
        extends Transformer2
  }
}
