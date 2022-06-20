package app

import io.circe.{Decoder, Encoder}

import scala.util.parsing.combinator.{PackratParsers, RegexParsers}
import scala.util.parsing.input.CharSequenceReader

sealed trait Getter { self =>
  import Getter._

  def extract(from: Type): Option[Type] =
    (self, from) match {
      case (AndThen(fst, snd), t)                        => fst.extract(t).flatMap(snd.extract)
      case (GetTupleFirst, Type.TTuple(first, _))        => Some(first)
      case (GetTupleSecond, Type.TTuple(_, second))      => Some(second)
      case (GetArrayElem(_), Type.TArray(elem))          => Some(elem)
      case (GetObjectField(field), Type.TObject(fields)) => fields.get(field)
      case _                                             => None
    }

  def show: String = self match {
    case AndThen(first, second) => s"$first$second"
    case GetArrayElem(index)    => s"[$index]"
    case GetObjectField(field)  => s"[\"$field\"]"
    case GetTupleFirst          => "_1"
    case GetTupleSecond         => "_2"
  }
}

object Getter {

  final case class AndThen(first: Getter, second: Getter) extends Getter
  final case class GetArrayElem(index: Int)               extends Getter
  final case class GetObjectField(field: String)          extends Getter
  case object GetTupleFirst                               extends Getter
  case object GetTupleSecond                              extends Getter

  implicit val encoder: Encoder[Getter] = Encoder[String].contramap(_.show)

  implicit val decoder: Decoder[Getter] = Decoder[String].emap(parsing.parse)

  private object parsing extends RegexParsers with PackratParsers {
    val number = """(0|[1-9]\d*)""".r ^^ { _.toInt }

    val qoutedString = "\"" ~ "[^\"]*".r ~ "\"" ^^ { case (_ ~ str ~ _) => str }

    lazy val getTupleFirst: PackratParser[Getter] = "_1" ^^ { _ =>
      GetTupleFirst
    }

    lazy val getTupleSecond: PackratParser[Getter] = "_2" ^^ { _ =>
      GetTupleSecond
    }

    lazy val getArraryElem: PackratParser[Getter] = "[" ~ number ~ "]" ^^ { case (_ ~ n ~ _) =>
      GetArrayElem(n)
    }

    lazy val getObjectField: PackratParser[Getter] = "[" ~ qoutedString ~ "]" ^^ { case (_ ~ str ~ _) =>
      GetObjectField(str)
    }

    lazy val andThen: PackratParser[Getter] = getter ~ getter ^^ { case (f ~ s) =>
      AndThen(f, s)
    }

    lazy val getter: PackratParser[Getter] =
      andThen | getTupleFirst | getTupleSecond | getArraryElem | getObjectField

    def parse(in: String): Either[String, Getter] = {
      val tokens = new PackratReader(new CharSequenceReader(in))
      phrase(getter)(tokens) match {
        case Success(result, _) => Right(result)
        case e: NoSuccess       => Left(e.msg)
      }
    }
  }

}
