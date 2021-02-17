package app.api

import app.flows.FlowRunner
import app.forms.FormElement.TextField
import app.forms._
import korolev.server.{KorolevServiceConfig, StateLoader}
import korolev.state.javaSerialization._
import korolev.zio.zioEffectInstance
import org.http4s.HttpRoutes
import zio._
import zio.interop.catz._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import app.forms.FormElement.NumberField

final class GeneratedFormsEndpoint[R <: GeneratedFormsEndpoint.Env] extends Endpoint[R] {
  import levsha.dsl._
  import html._
  import GeneratedFormsEndpoint.internal._

  val routes: URIO[R, HttpRoutes[RIO[R, *]]] =
    ZIO.runtime[R].map { implicit runtime =>
      import korolev._
      implicit val ec: ExecutionContext = runtime.platform.executor.asEC
      implicit val effect               = zioEffectInstance[R, Throwable](runtime)(identity)(identity)

      val ctx = Context[RIO[R, *], State, Any]
      import ctx._

      val config = KorolevServiceConfig[RIO[R, *], State, Any](
        stateLoader = StateLoader.default(State.Impossible),
        rootPath = "/",
        document = {
          case State.Found(_, formDefinition) =>
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
                        case (inputId, FormElement.TextField(_, name)) =>
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
                                      case TextField(_, _) =>
                                        property.get("value")
                                      case NumberField(_, _) =>
                                        property.get("value").flatMap(str => Task(str.toLong))
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
          case State.Submitted                =>
            optimize {
              Html(
                body(
                  "form has been submitted"
                )
              )
            }
          case State.Impossible               =>
            throw new IllegalStateException()
        },
        router = korolev.Router(
          fromState = {
            case State.Found(id, _) =>
              korolev.Root / "generated" / id.value.toString()
          },
          toState = {
            case korolev.Root / "generated" / id if id.nonEmpty =>
              _ =>
                for {
                  formId     <- Task(FormId(UUID.fromString(id))).mapError(_ => NotFoundError)
                  formWithId <- FormsRepository.get(formId).flatMap {
                                  _.fold[IO[NotFoundError.type, FormWithId]](ZIO.fail(NotFoundError))(ZIO.succeed(_))
                                }
                } yield State.Found(formId, formWithId)
            case _                                              => _ => ZIO.fail(NotFoundError)

          }
        )
      )
      NotFoundHandler(
        http4s.http4sKorolevService(config)
      )
    }

}

object GeneratedFormsEndpoint {
  type Env = FormsRepository with FlowRunner

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
      case object Submitted                                extends State
      case object Impossible                               extends State
      final case class Found(id: FormId, form: FormWithId) extends State
    }

  }

}
