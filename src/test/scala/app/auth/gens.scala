package app.auth

import zio.random.Random
import zio.test.{Gen, Sized}

object gens {

  val scope: Gen[Random with Sized, Scope] = {
    import Scope._

    val admin = Gen.const(Admin)

    val users = Gen.setOf(Gen.anyString).map(ForUsers(_))

    val perms = Gen.setOf(Gen.anyString).map(ForGroups(_))

    Gen.oneOf(admin, users, perms)

  }

}
