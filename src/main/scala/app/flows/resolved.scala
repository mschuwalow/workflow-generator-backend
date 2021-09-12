package app.flows

import app.Type
import app.forms.FormId
import app.jforms.JFormId

object resolved {
  final case class CreateFlowRequest(components: Map[ComponentId, Component])

  sealed trait Component { self =>
    import Component._

    def isSink: Boolean =
      self match {
        case FormOutput(_, _)  => false
        case JFormOutput(_, _) => false
        case Never(_)          => false
        case Numbers(_)        => false
        case Void(_, _)        => true
        case UDF(_, _, _, _)   => false
        case LeftJoin(_, _)    => false
        case InnerJoin(_, _)   => false
        case Merge(_, _)       => false
      }
  }

  object Component {

    // sources

    final case class FormOutput(formId: FormId, elementType: Type) extends Component

    final case class JFormOutput(formId: JFormId, elementType: Type) extends Component

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

  }
}
