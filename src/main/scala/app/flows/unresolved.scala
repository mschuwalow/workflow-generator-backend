package app.flows

import app.Type
import app.forms.FormId
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.annotation.unused

object unresolved {
  final case class CreateFlowRequest(components: Map[ComponentId, Component])

  object CreateFlowRequest {

    implicit val decoder: Decoder[CreateFlowRequest] =
      deriveDecoder

    implicit val encoder: Encoder[CreateFlowRequest] =
      deriveEncoder
  }

  sealed trait Component { self =>
    import Component._

    def isSink: Boolean =
      self match {
        case FormOutput(_)   => false
        case Never(_)        => false
        case Numbers(_)      => false
        case Void(_, _)      => true
        case UDF(_, _, _, _) => false
        case LeftJoin(_, _)  => false
        case InnerJoin(_, _) => false
        case Merge(_, _)     => false
      }
  }

  object Component {

    // sources

    final case class FormOutput(formId: FormId) extends Component

    final case class Never(elementType: Option[Type]) extends Component

    final case class Numbers(values: List[Long]) extends Component

    // sinks

    final case class Void(stream: ComponentId, elementType: Option[Type]) extends Component

    // transformers - 1 input

    final case class UDF(stream: ComponentId, code: String, inputTypeHint: Option[Type], outputTypeHint: Option[Type])
        extends Component

    // transformers - 2 inputs

    final case class LeftJoin(stream1: ComponentId, stream2: ComponentId) extends Component

    final case class InnerJoin(stream1: ComponentId, stream2: ComponentId) extends Component

    final case class Merge(stream1: ComponentId, stream2: ComponentId) extends Component

    @unused
    private implicit val configuration: Configuration =
      Configuration.default.withDiscriminator("type")

    implicit val encoder: Encoder[Component] =
      deriveConfiguredEncoder

    implicit val decoder: Decoder[Component] =
      deriveConfiguredDecoder

  }
}
