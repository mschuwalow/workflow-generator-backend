package app.forms

import app.auth.{gens => authGens}
import zio.random.Random
import zio.test.{Gen, Sized}

object gens {

  val formElementId: Gen[Random with Sized, FormElementId] = Gen.anyString.map(FormElementId(_))

  val formId: Gen[Random with Sized, FormId] = Gen.anyUUID.map(FormId(_))

  val formElement: Gen[Random with Sized, FormElement] = {
    import FormElement._

    val textField = for {
      id    <- formElementId
      label <- Gen.anyString
    } yield TextField(id, label)

    Gen.oneOf(textField)
  }

  val createFormRequest: Gen[Random with Sized, CreateFormRequest] =
    for {
      elements      <- Gen.listOf(formElement)
      scope         <- Gen.option(authGens.scope)
      uniqueElements = UniqueFormElements.make(elements.distinctBy(_.id)).runEither.toOption.get
    } yield CreateFormRequest(uniqueElements, scope)

}
