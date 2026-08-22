package external.fixtures.positive

import trading.quantity.*

object StaticEquivalenceRetagging:
  type A = Atom["equivalence:A"]
  type B = Atom["equivalence:B"]

  type AB = Times[A, B]
  type BA = Times[B, A]

  val a: DimRef[A] = DimRef.atom["equivalence:A"]
  val b: DimRef[B] = DimRef.atom["equivalence:B"]
  val ab: DimRef[AB] = DimRef.times(a, b)
  val equivalence: SameDimension[AB, BA] = summon
  val source: Quantity[AB]               = Quantity(ab, Rational.zero)
  val viaQuantity: Quantity[BA]          = source.alignTo[BA]

end StaticEquivalenceRetagging
