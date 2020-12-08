package app.repository

import zio._
import app.Database

object WorkflowRepository {

  trait Service extends Serializable {}

  val doobie: ZLayer[Database, Nothing, WorkflowRepository] = ???
}
