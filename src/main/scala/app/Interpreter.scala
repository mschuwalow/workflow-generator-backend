package app

import app.compiler.typed
import zio._

trait Interpreter {
  def run(flow: typed.Flow): Task[Unit]
}

object Interpreter {

  val stream: Interpreter = new Interpreter {

    def run(flow: typed.Flow): Task[Unit] =
      ZIO.foreachPar_(flow.streams)(_.run)
  }
}
