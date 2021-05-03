package app.infrastructure.udf

import zio._
import zio.duration._

import scala.reflect.ClassTag

trait Python {

  def runAs[A: ClassTag](
    startCommand: Port => Command,
    startDelay: Duration = Duration.Zero,
    shutdown: Option[A => UIO[Unit]] = None
  ): Managed[Throwable, A]
}

object Python {

  def runAs[A: ClassTag](
    startCommand: Port => Command,
    startDelay: Duration = Duration.Zero,
    shutdown: Option[A => UIO[Unit]] = None
  ): ZManaged[Has[Python], Throwable, A] =
    ZManaged.accessManaged(_.get.runAs(startCommand, startDelay, shutdown))
}
