package app

import zio.random.Random
import zio.test.Gen._
import zio.test.{Gen, Sized}

object gens {

  def tBool: Gen[Any, (String, Type)] = const(("Bool", Type.TBool))

  def tString: Gen[Any, (String, Type)] = const(("String", Type.TString))

  def tNumber: Gen[Any, (String, Type)] = const(("Number", Type.TNumber))

  def tDate: Gen[Any, (String, Type)] = const(("Date", Type.TDate))

  def primitiveType: Gen[Random, (String, Type)] = oneOf(tBool, tString, tNumber, tDate)

  def tArray: Gen[Random with Sized, (String, Type)] =
    anyType.map { case (s, t) =>
      (s"[$s]", Type.TArray(t))
    }

  def tObject: Gen[Random with Sized, (String, Type)] =
    Gen.listOfBounded(0, 3)(anyType).map { xs =>
      val names  = xs.zipWithIndex.map { case ((typeString, t), i) =>
        val field = s"field_${i}"
        (s""""$field": $typeString""", field, t)
      }
      val string = names.map(_._1).mkString("{", ", ", "}")
      val t      = Type.TObject(names.map { case (_, f, t) => (f, t) })
      (string, t)
    }

  def tTuple: Gen[Random with Sized, (String, Type)] =
    for {
      (s1, t1) <- anyType
      (s2, t2) <- anyType
    } yield (s"($s1, $s2)", Type.TTuple(t1, t2))

  def tEither: Gen[Random with Sized, (String, Type)] =
    for {
      (s1, t1) <- anyType
      (s2, t2) <- anyType
    } yield (s"($s1 | $s2)", Type.TEither(t1, t2))

  def tOption: Gen[Random with Sized, (String, Type)] =
    anyType.map { case (s, t) =>
      (s"$s?", Type.TOption(t))
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

  def anyType0: Gen[Random with Sized, Type] =
    anyType.map(_._2)
}
