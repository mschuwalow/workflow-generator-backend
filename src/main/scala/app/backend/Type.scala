package app.backend

import scala.util.parsing.combinator.PackratParsers
import scala.util.parsing.combinator.syntactical._

sealed abstract class Type

object Type {
  case object TNull                                 extends Type
  case object TBool                                 extends Type
  case object TString                               extends Type
  case object TNumber                               extends Type
  final case class TArray(elementType: Type)        extends Type
  final case class TObject(fields: (String, Type)*) extends Type
  final case class TOption(value: Type)             extends Type

  final case class TTuple(
    left: Type,
    right: Type)
      extends Type

  final case class TEither(
    left: Type,
    right: Type)
      extends Type

  val tNull: Type                            = TNull
  val tBool: Type                            = TBool
  val tString: Type                          = TString
  val tNumber: Type                          = TNumber
  def tArray(elementType: Type): Type        = TArray(elementType)
  def tObject(fields: (String, Type)*): Type = TObject(fields: _*)

  def tTuple(
    left: Type,
    right: Type
  ): Type = TTuple(left, right)

  def tEither(
    left: Type,
    right: Type
  ): Type                        = TEither(left, right)
  def tOption(value: Type): Type = TOption(value)

  def fromString(s: String): Either[String, Type] =
    parsing.parse(s)

  object parsing extends StandardTokenParsers with PackratParsers {
    lexical.reserved ++= List("Null", "Bool", "String", "Number")
    lexical.delimiters ++= List(
      "(",
      ")",
      "[",
      "]",
      "{",
      "}",
      ":",
      ",",
      "|",
      "?"
    )

    lazy val tNull: PackratParser[TNull.type] = "Null" ^^ { _ =>
      TNull
    }

    lazy val tBoolean: PackratParser[TBool.type] = "Bool" ^^ { _ =>
      TBool
    }

    lazy val tString: PackratParser[TString.type] = "String" ^^ { _ =>
      TString
    }

    lazy val tNumber: PackratParser[TNumber.type] = "Number" ^^ { _ =>
      TNumber
    }

    lazy val tArray: PackratParser[TArray] = "[" ~ fullType ~ "]" ^^ {
      case (_ ~ t ~ _) => TArray(t)
    }

    lazy val tObject: PackratParser[TObject] = "{" ~ repsep(
      stringLit ~ ":" ~ fullType,
      ","
    ) ~ "}" ^^ {
      case (_ ~ rawFields ~ _) =>
        val fields = rawFields.map { case (name ~ _ ~ t) => (name, t) }
        TObject(fields: _*)
    }

    lazy val tOption: PackratParser[TOption] = fullType ~ "?" ^^ {
      case (t ~ _) => TOption(t)
    }

    lazy val tTuple
      : PackratParser[TTuple] = "(" ~ fullType ~ "," ~ fullType ~ ")" ^^ {
      case (_ ~ t1 ~ _ ~ t2 ~ _) => TTuple(t1, t2)
    }

    lazy val tEither
      : PackratParser[TEither] = "(" ~ fullType ~ "|" ~ fullType ~ ")" ^^ {
      case (_ ~ t1 ~ _ ~ t2 ~ _) => TEither(t1, t2)
    }

    lazy val fullType
      : PackratParser[Type] = tOption | tNull | tBoolean | tString | tNumber | tArray | tObject | tTuple | tEither

    def parse(in: String): Either[String, Type] = {
      val tokens = new PackratReader(new lexical.Scanner(in))
      phrase(fullType)(tokens) match {
        case Success(result, _) => Right(result)
        case e                  => Left(e.toString())
      }
    }
  }
}
