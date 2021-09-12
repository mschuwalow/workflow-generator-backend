package app.jforms

import zio.{Has, URLayer}

package object inbound {

  val layer: URLayer[LiveJFormsService.Env, Has[JFormsService]] =
    LiveJFormsService.layer

}
