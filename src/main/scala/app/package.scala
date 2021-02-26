import app.api.Router
import app.config.HttpConfig
import app.postgres.Database
import zio.clock.Clock

package object app {
  type AppEnvironment = HttpConfig with Router.Env with Clock with Database
}
