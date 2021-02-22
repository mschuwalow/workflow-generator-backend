package app

import zio.Has

package object postgres {
  type Database = Has[Database.Service]
}
