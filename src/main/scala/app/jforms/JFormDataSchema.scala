package app.jforms

import app.Type
import cats.data.ValidatedNel
import cats.instances.either._
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import doobie.postgres.circe.jsonb.implicits._
import doobie.util.{Get, Put}
import io.circe.schema.{Schema, ValidationError}
import io.circe.{ACursor, Decoder, Encoder, Json}

import scala.util.Try

final case class JFormDataSchema(
  js: Json,
  elementType: Type
) {

  lazy val schema: Schema =
    Schema.load(js)

  def validate(value: Json): ValidatedNel[ValidationError, Unit] =
    schema.validate(value)
}

object JFormDataSchema {

  implicit val encoder: Encoder[JFormDataSchema] =
    Encoder.instance(_.js)

  implicit val decoder: Decoder[JFormDataSchema] =
    Decoder.decodeJson.emap(fromJson)

  implicit val put: Put[JFormDataSchema] =
    Put[Json].contramap(_.js)

  implicit val get: Get[JFormDataSchema] =
    Get[Json].map(fromJson(_).toOption.get)

  def fromJson(js: Json): Either[String, JFormDataSchema] =
    Try(Schema.load(js)).toEither.left.map(e => s"Failed to parse schema: $e") >>
      extractType(js.hcursor).map(JFormDataSchema(js, _))

  private[this] def extractType(hc: ACursor): Either[String, Type] =
    hc.get[String]("type").left.map(_.message).flatMap {
      case "object"             =>
        val fields = hc
          .downField("properties")
          .keys
          .toList
          .flatten
          .traverse(k => extractType(hc.downField("properties").downField(k)).map((k, _)))
        fields.map(fs => Type.TObject(fs.toMap))
      case "string"             =>
        hc.get[String]("format") match {
          case Right("date") => Right(Type.TDate)
          case _             => Right(Type.TString)
        }
      case "boolean"            => Right(Type.TBool)
      case "integer" | "number" => Right(Type.TNumber)
      case "array"              => extractType(hc.downField("items")).map(Type.TArray)
      case t                    => Left(s"Unsupported type: `$t`")
    }
}
