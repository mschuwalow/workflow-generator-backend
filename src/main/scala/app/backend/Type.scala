package app.backend

sealed abstract class Type

object Type {
  case object TBottom                               extends Type
  case object TNull                                 extends Type
  case object TBoolean                              extends Type
  case object TString                               extends Type
  case object TNumber                               extends Type
  final case class TArray(elementType: Type)        extends Type
  final case class TObject(fields: (String, Type)*) extends Type

  final case class TTuple(
    left: Type,
    right: Type)
      extends Type

  final case class TEither(
    left: Type,
    right: Type)
      extends Type

  val tBottom: Type                          = TBottom
  val tNull: Type                            = TNull
  val tBoolean: Type                         = TBoolean
  val tString: Type                          = TString
  val tNumber: Type                          = TNumber
  def tArray(elementType: Type): Type        = TArray(elementType)
  def tObject(fields: (String, Type)*): Type = TObject(fields: _*)

  def tTuple(
    left: Type,
    right: Type
  ): Type = TTuple(left, right)

  def tEither(
    left: Type,
    right: Type
  ): Type                        = TEither(left, right)
  def tOption(value: Type): Type = tEither(tNull, value)
}
