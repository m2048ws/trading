package external.fixtures.negative

import algebra.ring.AdditiveCommutativeMonoid

import trading.quantity.*
import trading.quantity.algebra.*
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.gridQuantityAlgebra.given
import trading.quantity.algebra.refinedAdditive.given
import trading.quantity.refinement.*

object AmbiguousDimensionAuthority:
  type D = Atom["authority:ambiguous"]
  sealed trait G

  given first: DimRef[D]  = DimRef.atom["authority:ambiguous"]
  given second: DimRef[D] = DimRef.atom["authority:ambiguous"]

  // OFFENDING-BEGIN
  val exactZero = Quantity.zero[D]
  val gridZero = GridQuantity.zero[D, G]
  val vector = summon[VectorSpace[Quantity[D], Rational]]
  val module = summon[LeftModule[GridQuantity[D, G], BigInt]]
  val monoid = summon[AdditiveCommutativeMonoid[NonNegative[Quantity[D]]]]
  // OFFENDING-END

end AmbiguousDimensionAuthority
