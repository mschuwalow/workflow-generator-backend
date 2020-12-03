package app

import app.compiler.Type
import zio.random.Random
import zio.test.Gen
import zio.test.Gen._
import zio.test.Sized

object gens {

  object backend {

    def tBool: Gen[Any, (String, Type)] = const(("Bool", Type.tBool))

    def tString: Gen[Any, (String, Type)] = const(("String", Type.tString))

    def tNumber: Gen[Any, (String, Type)] = const(("Number", Type.tNumber))

    def primitiveType: Gen[Random, (String, Type)] = oneOf(tBool, tString, tNumber)

    def tArray: Gen[Random with Sized, (String, Type)] =
      anyType.map { case (s, t) =>
        (s"[$s]", Type.tArray(t))
      }

    def tObject: Gen[Random with Sized, (String, Type)] =
      Gen.listOfBounded(0, 3)(anyType).map { xs =>
        val names = xs.zipWithIndex.map { case ((typeString, t), i) =>
          val field = s"field_${i}"
          (s""""$field": $typeString""", field, t)
        }
        val string = names.map(_._1).mkString("{", ", ", "}")
        val t      = Type.tObject(names.map { case (_, f, t) => (f, t) }: _*)
        (string, t)
      }

    def tTuple: Gen[Random with Sized, (String, Type)] =
      for {
        (s1, t1) <- anyType
        (s2, t2) <- anyType
      } yield (s"($s1, $s2)", Type.tTuple(t1, t2))

    def tEither: Gen[Random with Sized, (String, Type)] =
      for {
        (s1, t1) <- anyType
        (s2, t2) <- anyType
      } yield (s"($s1 | $s2)", Type.tEither(t1, t2))

    def tOption: Gen[Random with Sized, (String, Type)] =
      anyType.map { case (s, t) =>
        (s"$s?", Type.tOption(t))
      }

    def anyType: Gen[Random with Sized, (String, Type)] =
      small { size =>
        if (size > 1)
          oneOf(
            primitiveType,
            tArray,
            tObject,
            tTuple,
            tEither,
            tOption
          )
        else
          primitiveType
      }
  }
}
