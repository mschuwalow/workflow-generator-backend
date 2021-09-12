package app.forms

import doobie.postgres.circe.jsonb.implicits._
import doobie.util.{Get, Put}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import zio.prelude.SubtypeSmart
import zio.test.Assertion._

object UniqueFormElements extends SubtypeSmart[List[FormElement]](hasField("elementIds", _.map(_.id), isDistinct)) {

  implicit val encoder: Encoder[UniqueFormElements] =
    Encoder[List[FormElement]].contramap(unwrap)

  implicit val decoder: Decoder[UniqueFormElements] =
    Decoder[List[FormElement]].emap(make(_).toEither[String].left.map(_.mkString(", ")))

  implicit val uniqueFormElementsGet: Get[UniqueFormElements] =
    Get[Json].map(_.as[UniqueFormElements].toOption.get)

  implicit val uniqueFormElementsPut: Put[UniqueFormElements] =
    Put[Json].contramap(_.asJson)

}
