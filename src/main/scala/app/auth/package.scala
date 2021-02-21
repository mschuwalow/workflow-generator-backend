package app

import zio.Has

package object auth {

  type UserInfoService = Has[UserInfoService.Service]

}
