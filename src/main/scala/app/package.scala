import zio._

package object app {
  type Python    = Has[Python.Service]
  type Sys       = Has[Sys.Service]
  type UDFRunner = Has[UDFRunner.Service]
}
