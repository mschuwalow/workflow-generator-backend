package app.backend.nodes

import io.circe._
import app.backend.Type
import app.backend.Type._

object raw {
  sealed trait Component

  object Component {

    implicit val decoder: Decoder[Component] =
      Decoder.instance { hCursor =>
        hCursor.get[ComponentId]("type").flatMap {
          case Source.Id       => Right(Dummy)
          case Sink.Id         => Right(Dummy)
          case Transformer1.Id => Right(Dummy)
          case Transformer2.Id => Right(Dummy)
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

  final case class Source(sourceOp: SourceOp) extends Component

  object Source {
    val Id = ComponentId("Source")
  }

  final case class Sink(
    stream: ComponentId,
    sinkOp: SinkOp)
      extends Component

  object Sink {
    val Id = ComponentId("Sink")
  }

  final case class Transformer1(
    stream: ComponentId,
    op: TransformerOp1)
      extends Component {}

  object Transformer1 {
    val Id = ComponentId("Transformer1")
  }

  final case class Transformer2(
    stream1: ComponentId,
    stream2: ComponentId,
    op: TransformerOp2)
      extends Component {}

  object Transformer2 {
    val Id = ComponentId("Transformer2")
  }

  // todo: remove
  object Dummy extends Component {
    val ctype: Type = TBoolean
  }
}
