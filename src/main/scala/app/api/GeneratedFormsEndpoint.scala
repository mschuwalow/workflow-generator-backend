package app.api

import app.auth.{Permissions, UserInfo}
import app.flows.FlowRunner
import app.forms.FormElement.{TextField, _}
import app.forms._
import korolev.effect.Effect
import korolev.server.{KorolevService, KorolevServiceConfig}
import korolev.state.javaSerialization._
import korolev.{/, Context}
import zio._

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext

final class GeneratedFormsEndpoint[R <: GeneratedFormsEndpoint.Env](mountPath: String) extends KorolevEndpoint[R] {
  import GeneratedFormsEndpoint.internal._

  def makeService(implicit effect: Effect[RTask], ec: ExecutionContext): KorolevService[RTask] = {
    val ctx = Context[RTask, State, Any]
    import levsha.dsl._
    import html._
    import ctx._

    val config = KorolevServiceConfig[RIO[R, *], State, Any](
      rootPath = s"${mountPath}/",
      stateLoader = authedStateLoader {
        case (_, request, userInfo) =>
          request.pq match {
            case korolev.Root / id if id.nonEmpty =>
              for {
                formId     <- Task(FormId(UUID.fromString(id))).orElse(notFound)
                formWithId <- FormsRepository.get(formId).flatMap {
                                _.fold[Task[FormWithId]](notFound)(ZIO.succeed(_))
                              }
                _          <- formWithId.perms.fold[RTask[Unit]](ZIO.unit)(Permissions.authorize(userInfo, _))
              } yield State.Working(formId, formWithId, userInfo)
            case _                                =>
              notFound
          }
      },
      document = {
        case State.Working(_, formDefinition, _) =>
          val formId      = elementId()
          val definitions = formDefinition.elements.map(e => (elementId(), e))
          optimize {
            Html(
              body(
                form(
                  formId,
                  div(
                    legend("Form"),
                    definitions.map {
                      case (inputId, FormElement.TextField(_, name))   =>
                        p(
                          label(name),
                          input(
                            inputId,
                            `type` := "text"
                          )
                        )
                      case (inputId, FormElement.NumberField(_, name)) =>
                        p(
                          label(name),
                          input(
                            inputId,
                            `type` := "number"
                          )
                        )
                      case (inputId, FormElement.DatePicker(_, name))  =>
                        p(
                          label(name),
                          input(
                            inputId,
                            `type` := "date"
                          )
                        )
                    },
                    p(
                      button(
                        "Submit",
                        event("click") {
                          access =>
                            val elements = ZIO
                              .foreach(definitions) {
                                case (inputId, element) =>
                                  val property = access.property(inputId)
                                  val out      = element match {
                                    case TextField(_, _)   =>
                                      property.get("value")
                                    case NumberField(_, _) =>
                                      property.get("value").flatMap(str => Task(str.toLong))
                                    case DatePicker(_, _)  =>
                                      for {
                                        value  <- property.get("value")
                                        parsed <- Task(LocalDate.parse(value))
                                      } yield parsed
                                  }
                                  out.map((element.id.value, _))
                              }
                              .map(_.toMap)
                            val emit     = elements.flatMap { event =>
                              val outputType = formDefinition.outputType
                              FlowRunner
                                .emitFormOutput(formDefinition.id, outputType)(event.asInstanceOf[outputType.Scala])
                            }
                            emit *> access.transition(_ => State.Submitted)
                        }
                      )
                    )
                  )
                )
              )
            )
          }
        case State.Submitted                            =>
          optimize {
            Html(
              body(
                "form has been submitted"
              )
            )
          }
      },
      router = korolev.Router(
        toState = _ => s => ZIO.succeed(s)
      )
    )
    korolev.server.korolevService(config)
  }
}

object GeneratedFormsEndpoint {
  type Env = FormsRepository with FlowRunner with Permissions with KorolevEndpoint.Env

  private[GeneratedFormsEndpoint] object internal {
    sealed trait State

    object State {
      final case class Working(id: FormId, form: FormWithId, userInfo: UserInfo) extends State
      case object Submitted                                                      extends State
    }
  }
}
