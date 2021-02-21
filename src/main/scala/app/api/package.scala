package app

import zio.Has

package object api {

  type Auth = Has[Auth.Service]

}
