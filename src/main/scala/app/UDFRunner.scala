package app

import scala.jdk.CollectionConverters._

import app.compiler.Type
import app.udf.{ JEither, PythonRunner }
import zio._
import zio.duration._
import zio.logging.Logging
import zio.logging.log

object UDFRunner {

  trait Service {

    def runPython(
      function: String,
      input: Type,
      output: Type
    )(
      arg: input.Scala
    ): Task[output.Scala]
  }

  def live(workers: Int): ZLayer[Sys with Python with Logging, Throwable, UDFRunner] = {
    final case class Request(
      function: String,
      input: Any,
      cb: Promise[Throwable, Any])

    def toJava(t: Type)(v: Any): Any = {
      import Type._
      t match {
        case TBool   => v
        case TString => v
        case TNumber => v
        case TArray(elementType) =>
          v.asInstanceOf[Chunk[Any]].map(toJava(elementType)(_)).toArray
        case TObject(_) =>
          v.asInstanceOf[Map[String, Any]].asJava
        case TOption(valueType) =>
          val opt = v.asInstanceOf[Option[Any]]
          opt match {
            case None        => null
            case Some(value) => toJava(valueType)(value)
          }
        case TTuple(leftType, rightType) =>
          val (left, right) = v.asInstanceOf[Tuple2[Any, Any]]
          (toJava(leftType)(left), toJava(rightType)(right))
        case TEither(leftType, rightType) =>
          val value = v.asInstanceOf[Either[Any, Any]]
          value match {
            case Left(value)  => new JEither(toJava(leftType)(value), null)
            case Right(value) => new JEither(toJava(rightType)(value), null)
          }
      }
    }

    def fromJava(
      t: Type,
      v: Any
    ): t.Scala = {
      import Type._
      t match {
        case TBool   => v.asInstanceOf[t.Scala] // Boolean
        case TString => v.asInstanceOf[t.Scala] // String
        case TNumber => v.asInstanceOf[t.Scala] // Long
        case TArray(elementType) =>
          val jvalue = v.asInstanceOf[Array[AnyRef]]
          Chunk.fromArray(jvalue).map(fromJava(elementType, _)).asInstanceOf[t.Scala]
        case TObject(_) =>
          v.asInstanceOf[java.util.HashMap[String, Any]].asScala.asInstanceOf[t.Scala]
        case TOption(valueType) =>
          val value = if (v == null) None else Some(fromJava(valueType, v))
          value.asInstanceOf[t.Scala]
        case TTuple(leftType, rightType) =>
          val (left, right) = v.asInstanceOf[Tuple2[AnyRef, AnyRef]]
          (fromJava(leftType, left), fromJava(rightType, right)).asInstanceOf[t.Scala]
        case TEither(leftType, rightType) =>
          val jvalue = v.asInstanceOf[JEither]
          val value =
            if (jvalue.isLeft()) Left(fromJava(leftType, jvalue.left))
            else Right(fromJava(rightType, jvalue.right))
          value.asInstanceOf[t.Scala]
      }
    }

    ZLayer.fromManaged {
      Queue.unbounded[Request].toManaged(_.shutdown).flatMap { requests =>
        Sys.extractResource("/start-server.py").flatMap { startServer =>
          val startWorker = Python
            .runAs[PythonRunner](
              p => s"python3 ${startServer.toAbsolutePath()} --port=$p",
              2.seconds
            )
            .mapM { runner =>
              log.info("Started python worker.") *>
                requests.take.flatMap { case Request(function, input, cb) =>
                  log.debug(s"running function: $function")
                  ZIO.effect {
                    runner.run_udf(function, input)
                  }.to(cb)
                }.forever
            }
          ZManaged.collectAllPar_(List.fill(workers)(startWorker.fork)).map { _ =>
            new Service {
              def runPython(
                function: String,
                input: Type,
                output: Type
              )(
                arg: input.Scala
              ): zio.Task[output.Scala] =
                for {
                  prom   <- Promise.make[Throwable, Any]
                  casted <- ZIO.effect(toJava(input)(arg))
                  _      <- requests.offer(Request(function, casted, prom))
                  result <- prom.await
                  result <- ZIO.effect(fromJava(output, result))
                } yield result
            }
          }
        }
      }
    }
  }

  def runPython1(
    function: String,
    input: Type,
    output: Type
  )(
    arg: input.Scala
  ): RIO[UDFRunner, output.Scala] =
    ZIO.accessM(_.get.runPython(function, input, output)(arg))

}
