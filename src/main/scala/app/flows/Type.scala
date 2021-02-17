package app.flows

import io.circe.{Decoder, Encoder}
import zio.Chunk

import java.time.LocalDate
import scala.util.parsing.combinator.PackratParsers
import scala.util.parsing.combinator.syntactical._

sealed abstract class Type { self =>
  type Scala

  def show: String = {
    import Type._
    self match {
      case TDate                => "Date"
      case TBool                => "Bool"
      case TNumber              => "Number"
      case TString              => "String"
      case TArray(elementType)  => s"[${elementType.show}]"
      case TObject(fields)      =>
        fields.map { case (name, t) => s""""$name": ${t.show}""" }
          .mkString("{", ", ", "}")
      case TOption(value)       => s"${value.show}?"
      case TTuple(left, right)  => s"(${left.show}, ${right.show})"
      case TEither(left, right) => s"(${left.show} | ${right.show})"
    }
  }

  override def toString(): String = show
}

object Type {
  type TDate = TDate.type
  object TDate extends Type {
    type Scala = LocalDate
  }

  type TBool = TBool.type

  case object TBool extends Type {
    type Scala = Boolean
  }

  type TString = TString.type

  case object TString extends Type {
    type Scala = Boolean
  }

  type TNumber = TNumber.type

  case object TNumber extends Type {
    type Scala = Long
  }

  final case class TArray(elementType: Type) extends Type {
    type Scala = Chunk[elementType.Scala]
  }

  final case class TObject(fields: List[(String, Type)]) extends Type {
    type Scala = Map[String, Any]
  }

  final case class TOption(value: Type) extends Type {
    type Scala = Option[value.Scala]
  }

  final case class TTuple(left: Type, right: Type) extends Type {
    type Scala = (left.Scala, right.Scala)
  }

  final case class TEither(left: Type, right: Type) extends Type {
    type Scala = Either[left.Scala, right.Scala]
  }

  def fromString(s: String): Either[String, Type] = parsing.parse(s)

  implicit val decoder: Decoder[Type] =
    Decoder[String].emap(fromString)

  implicit val encoder: Encoder[Type] =
    Encoder[String].contramap(_.show)

  object parsing extends StandardTokenParsers with PackratParsers {
    lexical.reserved ++= List("Null", "Bool", "String", "Number", "Date")
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

    lazy val tDate: PackratParser[TDate] = "Date" ^^ { _ =>
      TDate
    }

    lazy val tBoolean: PackratParser[TBool] = "Bool" ^^ { _ =>
      TBool
    }

    lazy val tString: PackratParser[TString] = "String" ^^ { _ =>
      TString
    }

    lazy val tNumber: PackratParser[TNumber] = "Number" ^^ { _ =>
      TNumber
    }

    lazy val tArray: PackratParser[TArray] = "[" ~ fullType ~ "]" ^^ {
      case (_ ~ t ~ _) =>
        TArray(t)
    }

    lazy val tObject: PackratParser[TObject] = "{" ~ repsep(
      stringLit ~ ":" ~ fullType,
      ","
    ) ~ "}" ^^ {
      case (_ ~ rawFields ~ _) =>
        val fields = rawFields.map { case (name ~ _ ~ t) => (name, t) }
        TObject(fields)
    }

    lazy val tOption: PackratParser[TOption] = fullType ~ "?" ^^ {
      case (t ~ _) =>
        TOption(t)
    }

    lazy val tTuple: PackratParser[TTuple] =
      "(" ~ fullType ~ "," ~ fullType ~ ")" ^^ {
        case (_ ~ t1 ~ _ ~ t2 ~ _) =>
          TTuple(t1, t2)
      }

    lazy val tEither: PackratParser[TEither] =
      "(" ~ fullType ~ "|" ~ fullType ~ ")" ^^ {
        case (_ ~ t1 ~ _ ~ t2 ~ _) =>
          TEither(t1, t2)
      }

    lazy val fullType: PackratParser[Type] =
      tOption | tBoolean | tString | tNumber | tDate | tArray | tObject | tTuple | tEither

    def parse(in: String): Either[String, Type] = {
      val tokens = new PackratReader(new lexical.Scanner(in))
      phrase(fullType)(tokens) match {
        case Success(result, _) => Right(result)
        case e                  => Left(e.toString())
      }
    }
  }
}
