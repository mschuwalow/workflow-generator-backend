package app

import app.compiler.Type
import zio._
import zio.logging.Logging
import app.udf.PythonRunner
import zio.duration._
import zio.logging.log

object UDFRunner {
  trait Service {
    def runPython1(function: String, input: Type, output: Type)(arg: input.Scala): Task[output.Scala]

    def runPython2(
      function: String,
      input1: Type,
      input2: Type,
      output: Type
    )(
      arg1: input1.Scala,
      arg2: input2.Scala
    ): Task[output.Scala]
  }

  def live(workers: Int): ZLayer[Sys with Python with Logging, Throwable, UDFRunner] = {
    sealed trait Request
    final case class Request1(function: String, input: Any, cb: Promise[Throwable, Any]) extends Request
    final case class Request2(function: String, input1: Any, input2: Any, cb: Promise[Throwable, Any]) extends Request

    ZLayer.fromManaged {
      Queue.unbounded[Request].toManaged(_.shutdown).flatMap { requests =>
        Sys.extractResource("/start-server.py").flatMap { startServer =>
          val startWorker = Python.runAs[PythonRunner](
            p => s"python3 ${startServer.toAbsolutePath()} --port=$p",
            2.seconds
          ).mapM { runner =>
            log.info("Started python worker.") *>
            requests.take.flatMap {
              case Request1(function, input, cb) =>
                ZIO.effect {
                  runner.run_udf_1(function, input)
                }.to(cb)
              case Request2(function, input1, input2, cb) =>
                ZIO.effect {
                  runner.run_udf_2(function, input1, input2)
                }.to(cb)
            }.forever
          }
          ZManaged.collectAllPar_(List.fill(workers)(startWorker.fork)).map { _ =>
            new Service {
              def runPython1(function: String, input: Type, output: Type)(arg: input.Scala): zio.Task[output.Scala] =
                for {
                  prom    <- Promise.make[Throwable, Any]
                  _       <- requests.offer(Request1(function, arg, prom))
                  result  <- prom.await
                  result  <- ZIO.effect(result.asInstanceOf[output.Scala])
                } yield result

              def runPython2(function: String, input1: Type, input2: Type, output: Type)(arg1: input1.Scala, arg2: input2.Scala): zio.Task[output.Scala] =
                for {
                  prom    <- Promise.make[Throwable, Any]
                  _       <- requests.offer(Request2(function, arg1, arg2, prom))
                  result  <- prom.await
                  result  <- ZIO.effect(result.asInstanceOf[output.Scala])
                } yield result
            }
          }
        }
      }
    }
  }

  def runPython1(
function: String, input: Type, output: Type  )(
    arg: input.Scala
  ): RIO[UDFRunner, output.Scala] = ZIO.accessM(_.get.runPython1(function, input, output)(arg))

  def runPython2(
      function: String,
      input1: Type,
      input2: Type,
      output: Type
    )(
    arg1: input1.Scala,
    arg2: input2.Scala
  ): RIO[UDFRunner, output.Scala] = ZIO.accessM(_.get.runPython2(function, input1, input2, output)(arg1, arg2))

}
