package workflow.flows

final case class Workflow[F[+_]](
  id: F[WorkflowId],
  sinks: List[Graph.Sink])

object Graph {
  sealed trait SinkOp

  object SinkOp {
    case object SendEmail extends SinkOp
  }

  final case class Sink(
    stream: Stream,
    sinkOp: SinkOp)

  sealed trait Stream

  object Stream {
    sealed trait Source extends Stream
    object Source {}
    sealed trait Transformer extends Stream

    object Transformer {

      final case class Transformer1(
        stream: Stream,
        op: TransformerOp1)
          extends Transformer

      final case class Transformer2(
        stream1: Stream,
        stream2: Stream,
        op: TransformerOp2)
          extends Transformer
    }
  }

  sealed trait TransformerOp1
  object TransformerOp1

  sealed trait TransformerOp2
  object TransformerOp2
}
