package app.flows.inbound

import app.flows.{typed => Out, unresolved => In}
import zio._

package object compiler {
  type Env = resolver.Env

  def compileRequest(request: In.CreateFlowRequest): RIO[Env, Out.CreateFlowRequest] =
    for {
      graph <- ZIO.fromEither(syntactic.checkCycles(request))
      graph <- resolver.resolve(graph)
      flow  <- ZIO.fromEither(semantic.typecheck(graph))
    } yield flow

}
