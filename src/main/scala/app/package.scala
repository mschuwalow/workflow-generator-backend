import zio._

package object app {
  type Database    = Has[Database.Service]
  type Python      = Has[Python.Service]
  type Sys         = Has[Sys.Service]
  type UDFRunner   = Has[UDFRunner.Service]
  type Interpreter = Has[Interpreter.Service]
}
