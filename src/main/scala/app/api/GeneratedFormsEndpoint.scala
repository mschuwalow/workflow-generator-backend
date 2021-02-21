package app.api

import app.flows.FlowRunner
import app.forms.FormElement.{TextField, _}
import app.forms._
import korolev.effect.Effect
import korolev.server.{KorolevService, KorolevServiceConfig, StateLoader}
import korolev.state.javaSerialization._
import korolev.{/, Context}
import zio._

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace

final class GeneratedFormsEndpoint[R <: GeneratedFormsEndpoint.Env] extends KorolevEndpoint[R] {
  import GeneratedFormsEndpoint.internal._

  def makeService(implicit effect: Effect[RTask], ec: ExecutionContext): KorolevService[RTask] = {
    val ctx = Context[RTask, State, Any]
    import levsha.dsl._
    import html._
    import ctx._

    val config = KorolevServiceConfig[RIO[R, *], State, Any](
      stateLoader = StateLoader {
        case (_, request) =>
          request.pq match {
            case korolev.Root / "generated" / id if id.nonEmpty =>
              for {
                formId     <- Task(FormId(UUID.fromString(id))).mapError(_ => NotFoundError)
                formWithId <- FormsRepository.get(formId).flatMap {
                                _.fold[IO[NotFoundError.type, FormWithId]](ZIO.fail(NotFoundError))(ZIO.succeed(_))
                              }
              } yield State.Working(formId, formWithId)
            case _                                              =>
              ZIO.fail(NotFoundError)
          }
      },
      rootPath = "/",
      document = {
        case State.Working(_, formDefinition) =>
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
        case State.Submitted                  =>
          optimize {
            Html(
              body(
                "form has been submitted"
              )
            )
          }
      }
    )
    korolev.server.korolevService(config)
  }
}

object GeneratedFormsEndpoint {
  type Env = FormsRepository with FlowRunner with Auth

  private[GeneratedFormsEndpoint] object internal {

    case object NotFoundError extends NoStackTrace

    object NotFoundHandler {
      import cats.data.{Kleisli, OptionT}
      import org.http4s.{Request, Response}

      def apply[R](
        k: Kleisli[OptionT[RIO[R, *], *], Request[RIO[R, *]], Response[RIO[R, *]]]
      ): Kleisli[OptionT[RIO[R, *], *], Request[RIO[R, *]], Response[RIO[R, *]]] =
        Kleisli { req =>
          OptionT {
            k.run(req).value.catchSome {
              case NotFoundError => ZIO.succeed(None)
            }
          }
        }
    }

    sealed trait State

    object State {
      case object Submitted extends State

      final case class Working(id: FormId, form: FormWithId) extends State
    }

  }

}
