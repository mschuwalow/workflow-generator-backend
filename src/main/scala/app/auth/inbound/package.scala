package app.auth

package object inbound {

  val layer =
    LiveJWTAuth.layer ++ LivePermissions.layer

}
