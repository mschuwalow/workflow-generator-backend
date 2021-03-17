package app

import zio._

package object flows {
  type FlowService    = Has[FlowService.Service]
  type FlowRunner     = Has[FlowRunner.Service]
}
