package app.infrastructure.kafka

import zio.UIO

final case class Committable[+A](value: A, commit: UIO[Unit])
