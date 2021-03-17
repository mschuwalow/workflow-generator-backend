package app.flows.udf

import zio._
import java.nio.file.Path

trait Sys {
  def runCommand(cmd: Command): Managed[Throwable, RunningProcess]

  def tmpDir: Managed[Throwable, Path]

  def extractResource(name: String): Managed[Throwable, Path]

  def runFromClassPath(
    name: String,
    args: String*
  ): Managed[Throwable, RunningProcess] =
    for {
      executable <- extractResource(name)
      _          <- ZIO.effect(executable.toFile.setExecutable(true)).toManaged_
      process    <- runCommand(s"${executable} ${args.mkString(" ")}")
    } yield process

  def freePort: IO[Nothing, Port]
}

object Sys {

  def runCommand(cmd: Command): ZManaged[Has[Sys], Throwable, RunningProcess] =
    ZManaged.accessManaged(_.get.runCommand(cmd))

  def freePort: URIO[Has[Sys], Port] =
    ZIO.accessM(_.get.freePort)

  def extractResource(name: String): ZManaged[Has[Sys], Throwable, Path] =
    ZManaged.accessManaged(_.get.extractResource(name))

  def tmpDir: ZManaged[Has[Sys], Throwable, Path] =
    ZManaged.accessManaged(_.get.tmpDir)

  def runFromClassPath(
    name: String,
    args: String*
  ): ZManaged[Has[Sys], Throwable, RunningProcess] =
    ZManaged.accessManaged(_.get.runFromClassPath(name, args: _*))
}
