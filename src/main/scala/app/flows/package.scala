package app

import zio._

package object flows {
  type FlowService    = Has[FlowService.Service]
  type FlowRepository = Has[FlowRepository.Service]
  type FlowRunner     = Has[FlowRunner.Service]
}
