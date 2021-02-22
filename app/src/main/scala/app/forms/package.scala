package app

import zio._

package object forms {
  type FormsRepository    = Has[FormsRepository.Service]
  type UniqueFormElements = UniqueFormElements.Type
}
