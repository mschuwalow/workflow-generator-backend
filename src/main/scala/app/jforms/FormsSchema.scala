package app.forms

import io.circe.Json
import io.circe.schema.Schema
import app.Type
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.either._
import io.circe.ACursor
import scala.util.Try
import cats.syntax.flatMap._
import cats.data.ValidatedNel
import io.circe.schema.ValidationError

final case class FormsSchema(
  js: Json,
  elementType: Type
) {

  lazy val schema: Schema =
    Schema.load(js)

  def validate(value: Json): ValidatedNel[ValidationError, Unit] =
    schema.validate(value)
}

object FormsSchema {

  def fromJson(js: Json): Either[String, FormsSchema] =
    Try(Schema.load(js)).toEither.left.map(e => s"Failed to parse schema: $e") >>
      extractType(js.hcursor).map(FormsSchema(js, _))

  protected def extractType(hc: ACursor): Either[String, Type] =
    hc.get[String]("type").left.map(_.message).flatMap {
      case "object" =>
        val fields = hc.downField("properties").keys.toList.flatten.traverse(k =>
          extractType(hc.downField("properties").downField(k)).map((k, _))
        )
        fields.map(Type.TObject.apply)
      case "string" =>
        hc.get[String]("format") match {
          case Right("date") => Right(Type.TDate)
          case _             => Right(Type.TString)
        }
      case "boolean" => Right(Type.TBool)
      case "integer" | "number" => Right(Type.TNumber)
      case t => Left(s"Unsupported type: `$t`")
    }
}
