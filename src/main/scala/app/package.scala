import app.api.Router
import app.config.HttpConfig
import app.postgres.Database
import zio.clock.Clock
import zio.Has

package object app {
  type AppEnvironment = Has[HttpConfig] with Router.Env with Clock with Database
}
