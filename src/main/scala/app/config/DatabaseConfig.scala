package app.config

import pureconfig._
import pureconfig.generic.semiauto._

object DatabaseConfig {

  final case class Config(url: String, driver: String, user: String, password: String)

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }
}
