package app.forms.outbound

import app.forms.gens
import app.infrastructure.postgres.PostgresFormsRepository
import app.{BaseSpec, DatabaseAspect}
import zio.test.Assertion._
import zio.test._

object FormsRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec(name: String) =
    suite(name)(
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
      (baseSpec("Postgres implementation") @@ database).provideCustomLayerShared(PostgresLayer)
    )

  val PostgresLayer = DatabaseLayer >+> PostgresFormsRepository.layer

}
