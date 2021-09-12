package app.jforms.outbound

import app.infrastructure.postgres.PostgresJFormsRepository
import app.jforms.gens
import app.{BaseSpec, DatabaseAspect}
import zio.test.Assertion._
import zio.test._

object JFormsRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec(name: String) =
    suite(name)(
      testM("can save and get forms") {
        checkM(gens.createJFormRequest) { request =>
          db {
            for {
              r1 <- JFormsRepository.create(request)
              r2 <- JFormsRepository.get(r1.id)
            } yield assert(r2)(isSome(equalTo(r1))) && assert(r1.dataSchema)(equalTo(request.dataSchema)) && assert(
              r1.uiSchema
            )(equalTo(request.uiSchema)) && assert(r1.perms)(equalTo(request.perms))
          }
        }
      },
      testM("can save and get forms without option") {
        checkM(gens.createJFormRequest) { request =>
          db {
            for {
              r1 <- JFormsRepository.create(request)
              r2 <- JFormsRepository.getById(r1.id)
            } yield assert(r2)(equalTo(r1)) && assert(r1.dataSchema)(equalTo(request.dataSchema)) && assert(
              r1.uiSchema
            )(equalTo(request.uiSchema)) && assert(r1.perms)(equalTo(request.perms))
          }
        }
      },
      testM("fails when getting a nonexistant form") {
        checkM(gens.jformId) { formId =>
          db {
            for {
              result <- JFormsRepository.getById(formId).run
            } yield assert(result)(fails(equalTo(app.Error.NotFound)))
          }
        }
      }
    )

  def spec =
    suite("JFormsRepository")(
      (baseSpec("Postgres implementation") @@ database).provideCustomLayerShared(PostgresLayer)
    )

  val PostgresLayer = DatabaseLayer >+> PostgresJFormsRepository.layer

}
