package app

import app.flows.Type.TDate
import zio.random.Random
import zio.test.Gen._
import zio.test.{Gen, Sized}

object gens {

  object auth {
    import app.auth._

    val scope: Gen[Random with Sized, Scope] = {
      import Scope._

      val admin = Gen.const(Admin)

      val flows = Gen.const(Flows)

      val forms = Gen.const(Forms)

      val users = Gen.setOf(Gen.anyString).map(ForUsers(_))

      val perms = Gen.setOf(Gen.anyString).map(ForGroups(_))

      Gen.oneOf(admin, flows, forms, users, perms)

    }

  }

  object flows {
    import app.flows._

    object typed {
      import app.flows.typed._

      def stream: Gen[Random with Sized, Stream] = {
        import Stream._

        val never = for {
          id <- componentId
          t  <- anyType0
        } yield Never(id, t)

        val numbers = for {
          id <- componentId
          ns <- Gen.listOf(Gen.anyLong)
        } yield Numbers(id, ns)

        val innerJoin = Gen.suspend {
          for {
            id <- componentId
            s1 <- stream
            s2 <- stream
          } yield InnerJoin(id, s1, s2)
        }

        val leftJoin = Gen.suspend {
          for {
            id <- componentId
            s1 <- stream
            s2 <- stream
          } yield LeftJoin(id, s1, s2)
        }

        val merge = Gen.suspend {
          for {
            id <- componentId
            s1 <- stream
            s2 <- stream
          } yield Merge(id, s1, s2)
        }

        val udf = Gen.suspend {
          for {
            id   <- componentId
            code <- Gen.anyString
            s    <- stream
            t    <- anyType0
          } yield UDF(id, code, s, t)
        }

        Gen.oneOf(never, numbers, innerJoin, leftJoin, merge, udf)
      }

      val sink: Gen[Random with Sized, Sink] = {
        import Sink._
        val void = for {
          id <- componentId
          s  <- stream
        } yield Void(id, s)
        Gen.oneOf(void)
      }

      val flow: Gen[Random with Sized, Flow] =
        for {
          streams <- Gen.listOfBounded(0, 2)(sink)
        } yield Flow(streams)
    }

    def componentId: Gen[Random with Sized, ComponentId] =
      Gen.anyString.map(ComponentId(_))

    def flowId: Gen[Random, FlowId] =
      Gen.anyUUID.map(FlowId(_))

    def flowState: Gen[Random with Sized, FlowState] = {
      val running = Gen.const(FlowState.Running)
      val failed  = Gen.anyString.map(FlowState.Failed(_))
      val done    = Gen.const(FlowState.Done)
      Gen.oneOf(running, failed, done)
    }

    def tBool: Gen[Any, (String, Type)] = const(("Bool", Type.TBool))

    def tString: Gen[Any, (String, Type)] = const(("String", Type.TString))

    def tNumber: Gen[Any, (String, Type)] = const(("Number", Type.TNumber))

    def tDate: Gen[Any, (String, Type)] = const(("Date", TDate))

    def primitiveType: Gen[Random, (String, Type)] = oneOf(tBool, tString, tNumber, tDate)

    def tArray: Gen[Random with Sized, (String, Type)] =
      anyType.map {
        case (s, t) =>
          (s"[$s]", Type.TArray(t))
      }

    def tObject: Gen[Random with Sized, (String, Type)] =
      Gen.listOfBounded(0, 3)(anyType).map { xs =>
        val names  = xs.zipWithIndex.map {
          case ((typeString, t), i) =>
            val field = s"field_${i}"
            (s""""$field": $typeString""", field, t)
        }
        val string = names.map(_._1).mkString("{", ", ", "}")
        val t      = Type.TObject(names.map { case (_, f, t) => (f, t) })
        (string, t)
      }

    def tTuple: Gen[Random with Sized, (String, Type)] =
      for {
        (s1, t1) <- anyType
        (s2, t2) <- anyType
      } yield (s"($s1, $s2)", Type.TTuple(t1, t2))

    def tEither: Gen[Random with Sized, (String, Type)] =
      for {
        (s1, t1) <- anyType
        (s2, t2) <- anyType
      } yield (s"($s1 | $s2)", Type.TEither(t1, t2))

    def tOption: Gen[Random with Sized, (String, Type)] =
      anyType.map {
        case (s, t) =>
          (s"$s?", Type.TOption(t))
      }

    def anyType: Gen[Random with Sized, (String, Type)] =
      small { size =>
        if (size > 1)
          oneOf(
            primitiveType,
            tArray,
            tObject,
            tTuple,
            tEither,
            tOption
          )
        else
          primitiveType
      }

    def anyType0: Gen[Random with Sized, Type] =
      anyType.map(_._2)
  }

  object forms {
    import app.forms._

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

    val form: Gen[Random with Sized, Form] =
      for {
        elements      <- Gen.listOf(formElement)
        scope         <- Gen.option(auth.scope)
        uniqueElements = UniqueFormElements.make(elements.distinctBy(_.id)).runEither.toOption.get
      } yield Form(uniqueElements, scope)
  }
}
