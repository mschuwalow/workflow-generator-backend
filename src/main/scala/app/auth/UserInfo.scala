package app.auth

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

final case class UserInfo(
  username: String,
  perms: String
)

object UserInfo {

  implicit val encoder: Encoder.AsObject[UserInfo] =
    deriveEncoder

  implicit val decoder: Decoder[UserInfo] =
    deriveDecoder

}
