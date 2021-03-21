package app.flows

import app.BaseSpec
import app.gens.{flows => gens}
import app.postgres.{DatabaseAspect, PostgresFlowRepository}
import zio.test.Assertion._
import zio.test._

object FlowRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec =
    suite("Spec")(
      testM("can save and get flows") {
        checkM(gens.typed.flow) { flow =>
          db {
            for {
              r1 <- FlowRepository.save(flow)
              r2 <- FlowRepository.getById(r1.id)
            } yield assert(r1.streams)(equalTo(flow.streams)) && assert(r2.streams)(equalTo(flow.streams)) && assert(
              r1.id
            )(equalTo(r2.id))
          }
        }
      },
      testM("can get all flows") {
        checkM(gens.typed.flow) { flow =>
          db {
            for {
              r1     <- FlowRepository.save(flow)
              result <- FlowRepository.getAll
            } yield assert(result)(hasSameElements(List(r1)))
          }
        }
      },
      testM("can delete flows") {
        checkM(gens.typed.flow) { flow =>
          db {
            for {
              r1   <- FlowRepository.save(flow)
              _    <- FlowRepository.delete(r1.id)
              exit <- FlowRepository.getById(r1.id).run
            } yield assert(exit)(fails(anything))
          }
        }
      },
      testM("can set state") {
        checkM(gens.typed.flow, gens.flowState) { (flow, state) =>
          db {
            for {
              r1 <- FlowRepository.save(flow)
              _  <- FlowRepository.setState(r1.id, state)
              r2 <- FlowRepository.getById(r1.id)
            } yield assert(r2.state)(equalTo(state))
          }
        }
      }
    )

  def spec =
    suite("FlowsRepository")(
      suite("Postgres implementation")(
        baseSpec @@ database
      ).provideCustomLayerShared(PostgresLayer)
    )

  val PostgresLayer = DatabaseLayer >+> PostgresFlowRepository.layer

}
