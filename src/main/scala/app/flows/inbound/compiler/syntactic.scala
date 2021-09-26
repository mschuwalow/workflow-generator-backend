package app.flows.inbound.compiler

import app.Error
import app.flows.{ComponentId, unresolved}
import cats.instances.either._
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.traverse._

private[inbound] object syntactic {

  def checkCycles(
    graph: unresolved.CreateFlowRequest
  ): Either[Error.GraphValidationFailed, unresolved.CreateFlowRequest] = {
    // all starting points to traverse the graph
    val sinks = graph.components.collect {
      case (id, component) if component.isSink =>
        id
    }.toList

    val result = sinks.traverse { id =>
      // every subgraph needs to be checked seperately
      checkComponent(id).run(Context.initial(graph.components))
    }.as(graph)

    result.left.map(Error.GraphValidationFailed(_))
  }

  private[syntactic] type Run[A] = Check[Context, String, A]

  private[syntactic] final case class Context(
    nodes: Map[ComponentId, unresolved.Component],
    position: List[ComponentId]
  )

  private[syntactic] object Context {

    def initial(nodes: Map[ComponentId, unresolved.Component]): Context =
      Context(nodes, Nil)

  }

  private[syntactic] def failWithMessage(msg: String): Run[Nothing]               =
    Check.getState[Context].flatMap { ctx =>
      Check.fail(s"Failed while checking ${ctx.position.reverse.map(_.value).mkString("->")}: $msg")
    }

  private[syntactic] def getComponent(id: ComponentId): Run[unresolved.Component] =
    Check.getState.flatMap { ctx =>
      ctx.nodes
        .get(id)
        .fold[Run[unresolved.Component]](
          Check.fail(s"Component with id ${id.value} not found")
        )(
          Check.done
        )
    }

  private[syntactic] def nest[A](id: ComponentId)(nested: Run[A]): Run[A]         =
    for {
      s <- Check.getState[Context]
      _ <- if (s.position.contains(id)) failWithMessage(s"Cycle detected: ${id.value}") else Check.unit[Context]
      _ <- Check.updateState[Context](old => old.copy(position = id +: old.position))
      a <- nested
      _ <- Check.updateState[Context](old => old.copy(position = old.position.tail))
    } yield a

  private[syntactic] def checkComponent(id: ComponentId): Run[Unit]               = {
    import unresolved.Component._
    nest(id) {
      getComponent(id).flatMap {
        case Never(_)                    => Check.unit
        case Numbers(_)                  => Check.unit
        case UDF(stream, _, _, _)        => checkComponent(stream)
        case InnerJoin(stream1, stream2) => checkComponent(stream1) *> checkComponent(stream2)
        case LeftJoin(stream1, stream2)  => checkComponent(stream1) *> checkComponent(stream2)
        case Merge(stream1, stream2)     => checkComponent(stream1) *> checkComponent(stream2)
        case Void(stream, _)             => checkComponent(stream)
        case FormOutput(_)               => Check.unit
        case JFormOutput(_)              => Check.unit
      }
    }
  }

}
