package app.jforms

import app.{BaseSpec, Type}
import io.circe.literal._
import zio.test.Assertion._
import zio.test.assert

object JFormDataSchemaSpec extends BaseSpec {

  val spec =
    suite("JFormsDataSchema")(
      test("should parse example") {
        val json         =
          json"""{
                  "type": "object",
                  "properties": {
                    "Nachname": {
                      "type": "string",
                      "isMoveable": "true",
                      "index": 1
                    },
                    "Vorname": {
                      "type": "string",
                      "isMoveable": "true",
                      "index": 2
                    },
                    "Adresse": {
                      "type": "string",
                      "isMoveable": "true",
                      "index": 3
                    },
                    "Postleitzahl": {
                      "type": "number",
                      "isMoveable": "true",
                      "index": 4
                    },
                    "Typ der Arbeit": {
                      "type": "string",
                      "isMoveable": "true",
                      "index": 5,
                      "enum": [
                        "Bachelorarbeit",
                        "Masterarbeit"
                      ]
                    },
                    "Zur Veröffentlichung freigeben": {
                      "type": "boolean",
                      "isMoveable": "true",
                      "index": 6
                    }
                  }
                }"""
        val expectedType = Type.TObject(
          Map(
            "Nachname"                       -> Type.TString,
            "Vorname"                        -> Type.TString,
            "Adresse"                        -> Type.TString,
            "Postleitzahl"                   -> Type.TNumber,
            "Typ der Arbeit"                 -> Type.TString,
            "Zur Veröffentlichung freigeben" -> Type.TBool
          )
        )
        assert(JFormDataSchema.fromJson(json))(isRight(equalTo(JFormDataSchema(json, expectedType))))
      }
    )

}
