package app.infrastructure.udf

import zio.UIO

final case class RunningProcess(await: UIO[Int], destroy: UIO[Unit], destroyForcibly: UIO[Unit])
