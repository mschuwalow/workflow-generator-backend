package app

import app.Constants
import app.infrastructure.postgres.Database
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.TestAspect
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment

trait DatabaseAspect extends Constants {

  final def database: TestAspect[Nothing, TestEnvironment, Nothing, Any] =
    sequential >>> repeats(2) >>> retries(2) >>> samples(3) >>> shrinks(0)

  def db[R <: Has[DatabaseScope], E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    DatabaseScope.managed(zio)

  final def DatabaseLayer                                                =
    Blocking.live >+>
      Clock.live >+>
      ConfigLayer.orDie >+>
      Database.layer.orDie >+>
      DatabaseScope.postgres

  final def NoDatabaseLayer                                              =
    DatabaseScope.noop

}
