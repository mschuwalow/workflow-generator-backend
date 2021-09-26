package app.flows.inbound

import app.flows._
import zio._

private[inbound] trait FlowRunner  {
  def run(flow: typed.Flow): Task[Unit]
}

private[inbound] object FlowRunner {

  def run(flow: typed.Flow): RIO[Has[FlowRunner], Unit] = ZIO.accessM(_.get.run(flow))

}
