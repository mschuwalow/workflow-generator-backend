package app.infrastructure.udf

import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.logging.{Logging, log}

import java.io.{Closeable, FileOutputStream, InputStream}
import java.lang.{Runtime => JRuntime}
import java.net.ServerSocket
import java.nio.file.{Files, Path}

private[udf] object sys {

  final case class RunningProcess(
    await: UIO[Int],
    destroy: UIO[Unit],
    destroyForcibly: UIO[Unit]
  )

  def runCommand(cmd: Command): ZManaged[Clock with Blocking with Logging, Throwable, RunningProcess] = {
    val runtime = JRuntime.getRuntime()

    def pump[R <: Clock with Blocking](
      stream: InputStream,
      send: String => URIO[R, Unit]
    ) = {
      val reader = new java.io.BufferedReader(
        new java.io.InputStreamReader(stream)
      )
      val next   = blocking.effectBlocking(reader.readLine())
      val ready  = ZIO.effect(reader.ready())

      def loop: ZIO[R, Throwable, Unit] = {
        import zio.duration._
        ready.flatMap { hasNext =>
          if (hasNext) next.flatMap(send) *> loop
          else loop.delay(20.milliseconds)
        }
      }
      loop.interruptible
    }

    def run(cmd: String) =
      for {
        proc <- blocking.effectBlocking(runtime.exec(cmd)).toManaged { p =>
                  blocking.effectBlocking {
                    blocking.effectBlocking(p.destroy()).ignore *>
                      blocking
                        .effectBlocking(p.waitFor)
                        .race(Task(p.destroyForcibly).delay(5.minutes))
                  }.catchAll(e =>
                    log.warn(
                      s"Waiting for $cmd failed with ${e.toString}."
                    )
                  )
                }
        _    <- pump(proc.getInputStream(), log.info(_)).fork
                  .toManaged(_.interrupt)
        _    <- pump(proc.getErrorStream(), log.warn(_)).fork
                  .toManaged(_.interrupt)
      } yield proc

    run(cmd).flatMap { p =>
      ZManaged.environment[Blocking with Logging].map { env =>
        RunningProcess(
          (blocking.effectBlocking(p.waitFor()) *> ZIO.effect(
            p.exitValue()
          )).catchAll(e => log.warn(s"Command failed with ${e.toString()}").as(1))
            .provide(env),
          blocking
            .effectBlocking(p.destroy())
            .unit
            .catchAll(e => log.warn(s"Stopping process failed with ${e.toString()}"))
            .provide(env),
          blocking
            .effectBlocking(p.destroyForcibly())
            .unit
            .catchAll(e => log.warn(s"Killing process failed with ${e.toString()}"))
            .provide(env)
        )
      }
    }
  }

  def extractResource(name: String): ZManaged[Logging, Throwable, Path] = {
    def fromCloseable[A <: Closeable](thunk: => A) =
      ZManaged.make(ZIO.effect(thunk)) { ac =>
        ZIO
          .effect(ac.close())
          .catchAll(e => log.warn(s"Closing closeable failed ${e.toString()}"))
      }

    for {
      dir         <- tmpDir
      is          <- fromCloseable(getClass().getResource(name).openStream())
      resourcePath = dir.resolve("resource")
      os          <- fromCloseable(new FileOutputStream(resourcePath.toFile()))
      _           <- ZIO.effect {
                       val buffer = new Array[Byte](2048)
                       var length = 0

                       while ({ length = is.read(buffer); length } != -1)
                         os.write(buffer, 0, length);
                       os.close()
                       is.close()
                     }.toManaged_
    } yield resourcePath
  }

  val tmpDir: ZManaged[Logging, Throwable, Path] = {
    import scala.reflect.io.Directory
    ZManaged.make {
      ZIO.effect {
        Files.createTempDirectory("zio-sys")
      }
    } { path =>
      ZIO
        .effect(new Directory(path.toFile()).deleteRecursively())
        .catchAll(_ =>
          log.warn(
            s"Deleting ${path.toAbsolutePath().toString()} failed."
          )
        )
    }
  }

  val freePort: ZIO[Any, Nothing, Int] =
    ZIO.bracket(
      ZIO.effectTotal(new ServerSocket(0)),
      (socket: ServerSocket) => ZIO.effectTotal(socket.close()),
      (socket: ServerSocket) => ZIO.effectTotal(socket.getLocalPort)
    )

  def runFromClassPath(
    name: String,
    args: String*
  ): ZManaged[Clock with Blocking with Logging, Throwable, RunningProcess] =
    for {
      executable <- extractResource(name)
      _          <- ZIO.effect(executable.toFile.setExecutable(true)).toManaged_
      process    <- runCommand(s"${executable} ${args.mkString(" ")}")
    } yield process

}
