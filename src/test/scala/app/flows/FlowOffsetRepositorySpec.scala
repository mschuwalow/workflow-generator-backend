package app.flows

import app.BaseSpec
import app.gens.{flows => gens}
import app.postgres.{DatabaseAspect, PostgresFlowOffsetRepository, PostgresFlowRepository}
import zio.test.Assertion._
import zio.test._

object FlowOffsetRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec =
    suite("Spec")(
      testM("can save and get offsets") {
        checkM(gens.typed.createFlowRequest) { request =>
          db {
            for {
              flow   <- FlowRepository.create(request)
              offset <- gens.flowOffsetForFlow(flow.id).get
              _      <- FlowOffsetRepository.put(offset)
              r      <- FlowOffsetRepository.get(offset.flowId, offset.componentId)
            } yield assert(r)(equalTo(offset))
          }
        }
      },
      testM("can override offsets") {
        checkM(gens.typed.createFlowRequest) { request =>
          db {
            for {
              flow     <- FlowRepository.create(request)
              original <- gens.flowOffsetForFlow(flow.id).get
              updated   = original.copy(value = original.value + 1)
              _        <- FlowOffsetRepository.put(original)
              _        <- FlowOffsetRepository.put(updated)
              r        <- FlowOffsetRepository.get(updated.flowId, updated.componentId)
            } yield assert(r)(equalTo(updated))
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

  val PostgresLayer = DatabaseLayer >+> PostgresFlowRepository.layer >+> PostgresFlowOffsetRepository.layer

}
