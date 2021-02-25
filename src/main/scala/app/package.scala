import app.api.Router
import app.config.HttpConfig
import zio.clock.Clock

package object app {
  type AppEnvironment = HttpConfig with Router.Env with Clock
}
