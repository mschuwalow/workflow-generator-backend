package app.flows

import zio.UIO

final case class Committable[+A](value: A, commit: UIO[Unit])
