package workflow.config

import pureconfig._
import pureconfig.generic.semiauto._

object HttpConfig {

  final case class Config(
    port: Int,
    baseUrl: String)

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }
}
