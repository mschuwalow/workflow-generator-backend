package app

import app.compiler.typed
import zio._

object Interpreter {

  trait Service {
    def run(flow: typed.Flow): Task[Unit]
  }

  val stream: ZLayer[UDFRunner, Nothing, Interpreter] =
    ZLayer.fromFunction { env =>
      new Service {
        def run(flow: typed.Flow): Task[Unit] =
          ZIO.foreachPar_(flow.streams)(_.run).provide(env)
      }
    }

  def run(flow: typed.Flow): RIO[Interpreter, Unit] = ZIO.accessM(_.get.run(flow))

}
