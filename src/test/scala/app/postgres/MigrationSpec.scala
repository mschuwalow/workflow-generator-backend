package app.postgres

import app.BaseSpec
import zio.test.Assertion._
import zio.test._

object PostgresMigrationSpec extends BaseSpec with DatabaseAspect {
  def spec =
    suite("Migrations")(
      testM("should succeed") {
        DatabaseScope.DbLock.withPermit {
          assertM(Database.migrate.run)(succeeds(anything))
        }
      } @@ database
    ).provideCustomLayerShared(DatabaseLayer)
}
