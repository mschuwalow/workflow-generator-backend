package app.backend.nodes

import io.circe._
import app.backend.Type
import app.backend.Type._
import app.backend.nodes.typed.Sink
import app.backend.nodes.typed.Transformer1
import app.backend.nodes.typed.Transformer2

object typed {

  sealed trait Component { self =>
    val ctype: Type

    def isSink: Boolean = self match {
      case Source(_, _) =>
        false
      case Sink(_, _) =>
        true
      case Transformer1(_, _, _) =>
        false
      case Transformer2(_, _, _, _) =>
        false
    }
  }

  final case class Source(
    sourceOp: SourceOp,
    ctype: Type)
      extends Component

  final case class Sink(
    stream: ComponentId,
    sinkOp: SinkOp)
      extends Component {
    val ctype: Type = Type.TBottom
  }

  final case class Transformer1(
    stream: ComponentId,
    op: TransformerOp1,
    ctype: Type)
      extends Component {}

  final case class Transformer2(
    stream1: ComponentId,
    stream2: ComponentId,
    op: TransformerOp2,
    ctype: Type)
      extends Component {}
}
