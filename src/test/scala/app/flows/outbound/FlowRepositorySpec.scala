package app.flows.outbound

import app.flows.gens
import app.infrastructure.postgres.PostgresFlowRepository
import app.{BaseSpec, DatabaseAspect}
import zio.test.Assertion._
import zio.test._

object FlowRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec(name: String) =
    suite(name)(
      testM("can save and get flows") {
        checkM(gens.typed.createFlowRequest) { request =>
          db {
            for {
              r1 <- FlowRepository.create(request)
              r2 <- FlowRepository.getById(r1.id)
            } yield assert(r1.streams)(equalTo(request.streams)) && assert(r2.streams)(
              equalTo(request.streams)
            ) && assert(
              r1.id
            )(equalTo(r2.id))
          }
        }
      },
      testM("can get all flows") {
        checkM(gens.typed.createFlowRequest) { request =>
          db {
            for {
              r1     <- FlowRepository.create(request)
              result <- FlowRepository.getAll
            } yield assert(result)(hasSameElements(List(r1)))
          }
        }
      },
      testM("can delete flows") {
        checkM(gens.typed.createFlowRequest) { request =>
          db {
            for {
              r1   <- FlowRepository.create(request)
              _    <- FlowRepository.delete(r1.id)
              exit <- FlowRepository.getById(r1.id).run
            } yield assert(exit)(fails(anything))
          }
        }
      },
      testM("can set state") {
        checkM(gens.typed.createFlowRequest, gens.flowState) { (request, state) =>
          db {
            for {
              r1 <- FlowRepository.create(request)
              _  <- FlowRepository.setState(r1.id, state)
              r2 <- FlowRepository.getById(r1.id)
            } yield assert(r2.state)(equalTo(state))
          }
        }
      }
    )

  def spec =
    suite("FlowsRepository")(
      (baseSpec("Postgres implementation") @@ database).provideCustomLayerShared(PostgresLayer)
    )

  val PostgresLayer = DatabaseLayer >+> PostgresFlowRepository.layer

}
