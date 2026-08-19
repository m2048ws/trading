package external.fixtures.negative

import trading.quantity.*

object IncorrectStaticSameDimension:
  type A = Atom["same:A"]
  type B = Atom["same:B"]

  val a: DimRef[A] = DimRef.atom["same:A"]
  val quantity: Quantity[A] = Quantity(a, 1)

  // OFFENDING-BEGIN
  val incorrect: SameDimension[A, B] = summon[SameDimension[A, B]]
  val unrelated: Quantity[B] = quantity.asDimension[B](using incorrect)
  // OFFENDING-END

end IncorrectStaticSameDimension
