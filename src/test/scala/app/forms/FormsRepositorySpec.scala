package app.forms

import app.BaseSpec
import app.gens.{forms => gens}
import app.postgres.{DatabaseAspect, PostgresFormsRepository}
import zio.test.Assertion._
import zio.test._

object FormsRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec =
    suite("Spec")(
      testM("can save and get forms") {
        checkM(gens.createFormRequest) { request =>
          db {
            for {
              r1 <- FormsRepository.create(request)
              r2 <- FormsRepository.get(r1.id)
            } yield assert(r2)(isSome(equalTo(r1))) && assert(r1.elements)(equalTo(request.elements))
          }
        }
      },
      testM("can save and get forms without option") {
        checkM(gens.createFormRequest) { request =>
          db {
            for {
              r1 <- FormsRepository.create(request)
              r2 <- FormsRepository.getById(r1.id)
            } yield assert(r2)(equalTo(r1)) && assert(r1.elements)(equalTo(request.elements))
          }
        }
      },
      testM("fails when getting a nonexistant form") {
        checkM(gens.formId) { formId =>
          db {
            for {
              result <- FormsRepository.getById(formId).run
            } yield assert(result)(fails(equalTo(app.Error.NotFound)))
          }
        }
      }
    )

  def spec =
    suite("FormRepository")(
      suite("Postgres implementation")(
        baseSpec @@ database
      ).provideCustomLayerShared(PostgresLayer)
    )

  val PostgresLayer = DatabaseLayer >+> PostgresFormsRepository.layer

}
