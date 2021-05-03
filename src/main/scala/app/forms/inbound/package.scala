package app.forms

import zio.{Has, URLayer}

package object inbound {

  val layer: URLayer[LiveFormsService.Env, Has[FormsService]] =
    LiveFormsService.layer

}
