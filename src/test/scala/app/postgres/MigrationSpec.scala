package app.postgres

import app.BaseSpec
import zio.test.Assertion._
import zio.test._

object PostgresMigrationSpec extends BaseSpec with DatabaseAspect {
  def spec =
    suite("Migrations")(
      test("succeed") {
        assert(())(anything)
      } @@ database
    ).provideCustomLayerShared(DatabaseLayer)
}
