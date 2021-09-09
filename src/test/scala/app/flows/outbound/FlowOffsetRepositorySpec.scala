package app.flows.outbound

import app.flows.gens
import app.infrastructure.postgres.{PostgresFlowOffsetRepository, PostgresFlowRepository}
import app.{BaseSpec, DatabaseAspect}
import zio.test.Assertion._
import zio.test._

object FlowOffsetRepositorySpec extends BaseSpec with DatabaseAspect {

  def baseSpec(name: String) =
    suite(name)(
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
      (baseSpec("Postgres implementation") @@ database).provideCustomLayerShared(PostgresLayer)
    )

  val PostgresLayer = DatabaseLayer >+> PostgresFlowRepository.layer >+> PostgresFlowOffsetRepository.layer

}
