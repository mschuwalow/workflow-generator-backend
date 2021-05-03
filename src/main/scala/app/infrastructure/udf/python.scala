package app.infrastructure.udf

import py4j.ClientServer
import py4j.ClientServer.ClientServerBuilder
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.logging.Logging

import scala.reflect.ClassTag

private[udf] object python {

  def runAs[A: ClassTag](
    startCommand: Port => Command,
    startDelay: Duration,
    shutdown: Option[A => UIO[Unit]] = None
  ): ZManaged[Clock with Blocking with Logging with Clock, Throwable, A] = {

    val retrySchedule =
      Schedule.recurs(5) && Schedule.spaced(200.milliseconds)

    val startPythonServer = {
      val startTask = for {
        pythonPort <- sys.freePort.toManaged_
        _          <- sys.runCommand(startCommand(pythonPort))
        _          <- clock.sleep(startDelay).toManaged_
      } yield pythonPort

      startTask.retry(retrySchedule)
    }

    def startClientServer(pythonPort: Port) = {
      val startTask = sys.freePort.flatMap { jvmPort =>
        blocking.effectBlocking {
          new ClientServerBuilder()
            .pythonPort(pythonPort)
            .javaPort(jvmPort)
            .build()
        }
      }

      ZManaged.make {
        startTask.retry(retrySchedule)
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
  }

}
