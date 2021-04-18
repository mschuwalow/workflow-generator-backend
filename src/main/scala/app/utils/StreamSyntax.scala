package app.utils

import zio._
import zio.stream.ZStream

trait StreamSyntax {

  implicit class StreamOps[R, E, A](self: ZStream[R, E, A]) {

    def onFirstPull[R1 <: R, E1 >: E](effect: ZIO[R1, E1, Any]): ZStream[R1, E1, A] =
      ZStream {
        Ref.make(false).toManaged_.flatMap { state =>
          self.process.map { p =>
            state.get.flatMap {
              case true  =>
                p
              case false =>
                effect.option *> p
            }
          }
        }
      }
  }
}
