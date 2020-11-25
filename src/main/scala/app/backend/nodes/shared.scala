package app.backend.nodes

import app.backend.Type

import io.circe._
  final case class ComponentId(value: String) extends AnyVal

  object ComponentId {

    implicit val encoder: Encoder[ComponentId] =
      Encoder[String].contramap(_.value)

    implicit val decoder: Decoder[ComponentId] =
      Decoder[String].map(ComponentId.apply)

    implicit val keyDecoder: KeyDecoder[ComponentId] =
      KeyDecoder[String].map(ComponentId.apply)
  }

  sealed trait SourceOp {
    val outType: Type
  }
  object SourceOp {}

  sealed trait SinkOp {
    val inType: Type
  }

  object SinkOp {}

  sealed trait TransformerOp1
  object TransformerOp1 {
    object Identity extends TransformerOp1
    final case class UDF(
      inputTypeHint: Option[Type]
    ) extends TransformerOp1
  }

  sealed trait TransformerOp2
  object TransformerOp2 {
    object LeftJoin extends TransformerOp2
    object InnerJoin extends TransformerOp2
    final case class UDF(
      input1TypeHint: Option[Type],
      input2TypeHint: Option[Type]
    ) extends TransformerOp2
  }
