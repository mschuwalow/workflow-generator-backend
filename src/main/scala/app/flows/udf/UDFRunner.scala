package app.flows.udf

import app.flows.Type
import zio._

trait UDFRunner {

  def runPython(
    function: String,
    input: Type,
    output: Type
  )(
    arg: input.Scala
  ): Task[output.Scala]
}

object UDFRunner {

  def runPython(
    function: String,
    input: Type,
    output: Type
  )(
    arg: input.Scala
  ): RIO[Has[UDFRunner], output.Scala] =
    ZIO.accessM(_.get.runPython(function, input, output)(arg))
}
