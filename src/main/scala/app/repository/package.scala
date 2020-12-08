package app

import zio._

package object repository {
  type WorkflowRepository = Has[WorkflowRepository.Service]
}
