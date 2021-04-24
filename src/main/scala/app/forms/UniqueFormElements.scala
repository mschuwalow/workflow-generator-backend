package app.forms

import io.circe.{Decoder, Encoder}
import zio.prelude.SubtypeSmart
import zio.test.Assertion._

object UniqueFormElements extends SubtypeSmart[List[FormElement]](hasField("elementIds", _.map(_.id), isDistinct)) {

  implicit val encoder: Encoder[UniqueFormElements] =
    Encoder[List[FormElement]].contramap(unwrap)

  implicit val decoder: Decoder[UniqueFormElements] =
    Decoder[List[FormElement]].emap(make(_).mapError(_.mkString(", ")).runEither)

}
