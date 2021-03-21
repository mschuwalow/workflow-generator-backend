import app.api.Router
import app.config.HttpConfig
import zio.Has
import zio.clock.Clock

package object app {
  type AppEnvironment = Router.Env with Clock with Has[HttpConfig]
}
