package app

import zio.test.Gen
import zio.{URIO, ZIO}

trait GensSyntax {

  final implicit class Syntax[R, A](gen: Gen[R, A]) {

    def get: URIO[R, A] =
      gen.runHead.get <> ZIO.dieMessage("Unable to sample element.")

  }

}
