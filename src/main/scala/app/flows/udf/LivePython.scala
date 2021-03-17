package app.flows.udf

import py4j.ClientServer
import py4j.ClientServer.ClientServerBuilder
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.logging.Logging

import scala.reflect.ClassTag

private final class LivePython(
  env: LivePython.Env
) extends Python {
  def retry[R <: Clock, E, A] =
    (zio: ZIO[R, E, A]) => zio.retry(Schedule.recurs(5) && Schedule.spaced(200.milliseconds))

  def runAs[A: ClassTag](
    startCommand: Port => Command,
    startDelay: Duration,
    shutdown: Option[A => UIO[Unit]]
  ): Managed[Throwable, A] = {

    val retrySchedule =
      Schedule.recurs(5) && Schedule.spaced(200.milliseconds)

    val startPythonServer = {
      val startTask = for {
        pythonPort <- Sys.freePort.toManaged_
        _          <- Sys.runCommand(startCommand(pythonPort))
        _          <- clock.sleep(startDelay).toManaged_
      } yield pythonPort

      startTask.retry(retrySchedule)
    }

    def startClientServer(pythonPort: Port) = {
      val startTask = Sys.freePort.flatMap { jvmPort =>
        blocking.effectBlocking {
          new ClientServerBuilder()
            .pythonPort(pythonPort)
            .javaPort(jvmPort)
            .build()
        }
      }

      ZManaged.make {
        retry(startTask)
      } { client =>
        ZIO.effect(client.shutdown()).either
      }
    }


    def cast(client: ClientServer): Managed[Throwable, A] =
      ZManaged.make {
        ZIO.effect {
          client
            .getPythonServerEntryPoint(
              Array(implicitly[ClassTag[A]].runtimeClass)
            )
            .asInstanceOf[A]
        }
      } { a =>
        shutdown.map(_(a)).getOrElse(ZIO.unit)
      }

    for {
      pythonPort <- startPythonServer
      client     <- startClientServer(pythonPort)
      instance   <- cast(client)
    } yield instance
  }.provide(env)
}

object LivePython {
  type Env = Has[Sys] with Logging with Clock with Blocking

  val layer: URLayer[Env, Has[Python]] = {
    for {
      env <- ZManaged.environment[Env]
    } yield new LivePython(env)
  }.toLayer
}
