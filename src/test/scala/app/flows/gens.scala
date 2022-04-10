package app.flows

import app.Type
import app.gens.anyType0
import zio.random.Random
import zio.test.{Gen, Sized}

object gens {

  object typed {
    import app.flows.typed._

    def streamWithType(requestedType: Type): Gen[Random with Sized, Stream] = {
      import Stream._

      val never = for {
        id <- componentId
      } yield Never(id, requestedType)

      val merge = Gen.suspend {
        for {
          id <- componentId
          s1 <- streamWithType(requestedType)
          s2 <- streamWithType(requestedType)
        } yield Merge(id, s1, s2)
      }

      val udf = Gen.suspend {
        for {
          id   <- componentId
          code <- Gen.anyString
          s    <- stream
        } yield UDF(id, code, s, requestedType)
      }

      Gen.oneOf(never, merge, udf)
    }

    def stream: Gen[Random with Sized, Stream] = {
      import Stream._

      val never = for {
        id <- componentId
        t  <- anyType0
      } yield Never(id, t)

      val numbers = for {
        id <- componentId
        ns <- Gen.listOf(Gen.anyLong)
      } yield Numbers(id, ns)

      val innerJoin = Gen.suspend {
        for {
          id <- componentId
          s1 <- stream
          s2 <- stream
        } yield InnerJoin(id, s1, s2)
      }

      val leftJoin = Gen.suspend {
        for {
          id <- componentId
          s1 <- stream
          s2 <- stream
        } yield LeftJoin(id, s1, s2)
      }

      val merge = Gen.suspend {
        for {
          id <- componentId
          s1 <- stream
          s2 <- stream
        } yield MergeEither(id, s1, s2)
      }

      val mergeEither = Gen.suspend {
        for {
          id <- componentId
          t  <- anyType0
          s1 <- streamWithType(t)
          s2 <- streamWithType(t)
        } yield Merge(id, s1, s2)
      }

      val udf = Gen.suspend {
        for {
          id   <- componentId
          code <- Gen.anyString
          s    <- stream
          t    <- anyType0
        } yield UDF(id, code, s, t)
      }

      Gen.oneOf(never, numbers, innerJoin, leftJoin, merge, mergeEither, udf)
    }

    val sink: Gen[Random with Sized, Sink] = {
      import Sink._
      val void = for {
        id <- componentId
        s  <- stream
      } yield Void(id, s)
      Gen.oneOf(void)
    }

    val createFlowRequest: Gen[Random with Sized, CreateFlowRequest] =
      for {
        streams <- Gen.listOfBounded(0, 2)(sink)
      } yield CreateFlowRequest(streams)

    val flow: Gen[Random with Sized, Flow] =
      for {
        id      <- flowId
        request <- createFlowRequest
        state   <- flowState
      } yield Flow(id, request.streams, state)
  }

  def componentId: Gen[Random with Sized, ComponentId] =
    Gen.anyString.map(ComponentId(_))

  def flowId: Gen[Random, FlowId] =
    Gen.anyUUID.map(FlowId(_))

  def flowState: Gen[Random with Sized, FlowState] = {
    val running = Gen.const(FlowState.Running)
    val failed  = Gen.anyString.map(FlowState.Failed(_))
    val done    = Gen.const(FlowState.Done)
    Gen.oneOf(running, failed, done)
  }

  def flowOffsetForFlow(flowId: FlowId): Gen[Random with Sized, FlowOffset] =
    for {
      componentId <- componentId
      offset      <- Gen.anyLong
    } yield FlowOffset(flowId, componentId, offset)

  def flowOffset: Gen[Random with Sized, FlowOffset] =
    for {
      flowId <- flowId
      offset <- flowOffsetForFlow(flowId)
    } yield offset

}
