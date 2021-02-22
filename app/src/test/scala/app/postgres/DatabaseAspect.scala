package app.postgres

import app.Constants
import doobie.implicits._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.test.TestAspect
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment

trait DatabaseAspect extends Constants {

  final def database: TestAspect[Nothing, Database with TestEnvironment, Nothing, Any] =
    around_(clean.orDie, clean.orDie) >>> sequential >>> samples(1) >>> shrinks(0)

  final def DatabaseLayer =
    Blocking.live >+>
      Clock.live >+>
      ConfigLayer.orDie >+>
      Database.live.orDie

  private[this] val clean = Database.getTransactor.flatMap { transactor =>
    val cleanFlows =
      sql"""DELETE FROM flows
           |""".stripMargin.update.run.transact(transactor)

    val cleanForms =
      sql"""DELETE FROM forms
           |""".stripMargin.update.run.transact(transactor)

    cleanFlows *> cleanForms
  }
}
