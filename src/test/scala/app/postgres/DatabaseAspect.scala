package app.postgres

import app.Constants
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.TestAspect
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment

trait DatabaseAspect extends Constants {
  type DatabaseTestManager = Has[DatabaseTestManager.Service]

  final def database: TestAspect[Nothing, TestEnvironment, Nothing, Any] =
    sequential >>> repeats(2) >>> retries(2) >>> samples(3) >>> shrinks(0)

  def db[R <: DatabaseTestManager, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    DatabaseTestManager.managed(zio)

  final def DatabaseLayer =
    Blocking.live >+>
      Clock.live >+>
      ConfigLayer.orDie >+>
      Database.live.orDie >+>
      DatabaseTestManager.live

  final def NoDatabaseLayer =
    DatabaseTestManager.fake

}
