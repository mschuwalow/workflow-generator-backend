package app

import scala.util.control.NoStackTrace

sealed abstract class Error extends NoStackTrace

object Error {

  final case class ValidationFailed(reason: String) extends Error

  case object NotFound extends Error

}
