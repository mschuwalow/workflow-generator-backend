package app.infrastructure.udf

import app.BaseSpec
import zio._
import zio.logging.Logging
import zio.test.Assertion._
import zio.test._

import java.nio.file.Path
import scala.io.Source

object SysSpec extends BaseSpec {

  def spec =
    suite("Sys")(
      testM("extractResource should extract resources and delete them") {
        Ref.make[Option[Path]](None).flatMap { ref =>
          sys.extractResource("/testFile").use { path =>
            ZIO.effect(Source.fromFile(path.toFile).getLines().toList).map { lines =>
              assert(lines)(equalTo(List("abc123")))
            } <* ref.set(Some(path))
          } *> ref.get.map(path => assert(path.get.toFile.exists)(isFalse))
        }
      },
      suite("runFromClassPath")(
        testM("should extract executable and run it") {
          sys.tmpDir.use { dir =>
            val filePath = dir.toAbsolutePath().toString() + "/foo.txt"
            sys.runFromClassPath("/writeToFile.sh", filePath).use(_.await) *>
              ZIO
                .effect(Source.fromFile(filePath).getLines().toList)
                .map(assert(_)(equalTo(List("foo"))))
          }
        },
        testM("should allow killing stray processes") {
          sys
            .runFromClassPath("/sleepForever.sh")
            .use(_.destroy.as(assertCompletes))
        }
      )
    ).provideSomeLayer(
      (ZLayer.identity[Environment] ++ Logging.ignore)
    )
}
