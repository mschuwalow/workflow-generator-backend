package app.flows

import zio._

package object udf {
  type UDFRunner = Has[UDFRunner.Service]
  type Python    = Has[Python.Service]
  type Sys       = Has[Sys.Service]
}
