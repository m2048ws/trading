package external.fixtures.negative

import trading.quantity.*

object WidenedLiteralAtom:
  type K = String & Singleton
  type D = Atom[K]

  val a: ValueOf[K] =
    new ValueOf("first")

  val b: ValueOf[K] =
    new ValueOf("second")

  // OFFENDING-BEGIN
  val first: DimRef[D] =
    DimRef.atom[K](using a)

  val second: DimRef[D] =
    DimRef.atom[K](using b)
  // OFFENDING-END

end WidenedLiteralAtom
