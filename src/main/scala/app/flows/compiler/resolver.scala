package app.flows.compiler

import app.flows._
import app.forms.FormsRepository
import zio._

object resolver {
  type Env = Has[FormsRepository]

  def resolve(input: unresolved.CreateFlowRequest): RIO[Env, resolved.CreateFlowRequest] = {
    val out = ZIO.foreach(input.components) { case (k, v) => resolveComponent(v).map((k, _)) }
    out.map(resolved.CreateFlowRequest)
  }

  private def resolveComponent(component: unresolved.Component): RIO[Env, resolved.Component] = {
    import unresolved.Component._
    import resolved.{Component => Out}
    component match {
      case FormOutput(formId)                               =>
        for {
          form <- FormsRepository.getById(formId)
        } yield Out.FormOutput(form.id, form.outputType)
      case Never(elementType)                               =>
        ZIO.succeed(Out.Never(elementType))
      case Numbers(values)                                  =>
        ZIO.succeed(Out.Numbers(values))
      case Void(stream, elementType)                        =>
        ZIO.succeed(Out.Void(stream, elementType))
      case UDF(stream, code, inputTypeHint, outputTypeHint) =>
        ZIO.succeed(Out.UDF(stream, code, inputTypeHint, outputTypeHint))
      case LeftJoin(stream1, stream2)                       =>
        ZIO.succeed(Out.LeftJoin(stream1, stream2))
      case InnerJoin(stream1, stream2)                      =>
        ZIO.succeed(Out.InnerJoin(stream1, stream2))
      case Merge(stream1, stream2)                          =>
        ZIO.succeed(Out.Merge(stream1, stream2))
    }
  }

}
