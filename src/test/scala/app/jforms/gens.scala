package app.jforms

import app.auth.{gens => authGens}
import io.circe.Json
import io.circe.syntax._
import zio.random.Random
import zio.test.{Gen, Sized}

object gens {

  val jformId: Gen[Random with Sized, JFormId] = Gen.anyUUID.map(JFormId(_))

  val jformDataSchema: Gen[Any, JFormDataSchema] =
    Gen.const(JFormDataSchema.fromJson(Json.obj("type" -> "object".asJson, "properties" -> Json.obj())).toOption.get)

  val jformUiSchema: Gen[Any, Json]              = Gen.const(Json.obj())

  val createJFormRequest: Gen[Random with Sized, CreateJFormRequest] =
    for {
      dataSchema <- jformDataSchema
      uiSchema   <- jformUiSchema
      scope      <- Gen.option(authGens.scope)
    } yield CreateJFormRequest(dataSchema, uiSchema, scope)

}
