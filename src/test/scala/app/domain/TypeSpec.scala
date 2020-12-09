package app.compiler

import app.BaseSpec
import app.domain.Type
import app.domain.Type._
import app.gens.{ domain => gens }
import zio.test.Assertion._
import zio.test._
import io.circe.syntax._

object TypeSpec extends BaseSpec {

  val spec = suite("Type")(
    suite("parsing")(
      test("should parse `bool` primitive") {
        assert(Type.fromString("Bool"))(isRight(equalTo(TBool)))
      },
      test("should parse `string` primitive") {
        assert(Type.fromString("String"))(isRight(equalTo(TString)))
      },
      test("should parse `number` primitive") {
        assert(Type.fromString("Number"))(isRight(equalTo(TNumber)))
      },
      testM("should parse any well formed type") {
        check(gens.anyType) { case (str, t) =>
          assert(Type.fromString(str))(isRight(equalTo(t)))
        }
      }
    ),
    testM("circe roundtrip") {
      check(gens.anyType) { case (_, t) =>
        assert(t.asJson.as[Type])(isRight(equalTo(t)))
      }
    }
  )
}
