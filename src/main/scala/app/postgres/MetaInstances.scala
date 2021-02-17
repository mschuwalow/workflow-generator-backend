package app.postgres

import app.flows.typed
import app.forms.FormElement
import doobie.postgres.circe.jsonb.implicits._
import doobie.util.{Get, Put}
import io.circe.Json
import io.circe.syntax._

trait MetaInstances {

  implicit val typedSinkListGet: Get[List[typed.Sink]] =
    Get[Json].map(_.as[List[typed.Sink]].toOption.get)

  implicit val typedSinkListPut: Put[List[typed.Sink]] =
    Put[Json].contramap(_.asJson)

  implicit val formElementListGet: Get[List[FormElement]] =
    Get[Json].map(_.as[List[FormElement]].toOption.get)

  implicit val formElementListPut: Put[List[FormElement]] =
    Put[Json].contramap(_.asJson)

}
