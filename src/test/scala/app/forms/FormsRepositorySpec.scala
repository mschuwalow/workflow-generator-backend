package app.forms

import app.BaseSpec
import app.gens.{forms => gens}
import app.postgres.DatabaseAspect
import zio.test.Assertion._
import zio.test._

object FormsRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec =
    suite("Spec")(
      testM("can save and get forms") {
        checkM(gens.form) { form =>
          db {
            for {
              r1 <- FormsRepository.store(form)
              r2 <- FormsRepository.get(r1.id)
            } yield assert(r2)(isSome(equalTo(r1))) && assert(r1.elements)(equalTo(form.elements))
          }
        }
      },
      testM("can save and get forms without option") {
        checkM(gens.form) { form =>
          db {
            for {
              r1 <- FormsRepository.store(form)
              r2 <- FormsRepository.getById(r1.id)
            } yield assert(r2)(equalTo(r1)) && assert(r1.elements)(equalTo(form.elements))
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

  val PostgresLayer = DatabaseLayer >+> FormsRepository.doobie

}
