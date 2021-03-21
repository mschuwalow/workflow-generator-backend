package app

import doobie.util.transactor.Transactor
import zio.Task

package object postgres {

  type TaskTransactor = Transactor[Task]

}
