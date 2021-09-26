package app.auth

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class UserInfo(
  id: String,
  username: String,
  group: String
)

object UserInfo {

  implicit val encoder: Encoder.AsObject[UserInfo] =
    deriveEncoder

  implicit val decoder: Decoder[UserInfo]          =
    deriveDecoder

}
