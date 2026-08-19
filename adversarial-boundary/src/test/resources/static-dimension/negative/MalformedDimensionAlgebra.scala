package external.fixtures.negative

import algebra.ring.AdditiveCommutativeMonoid

import trading.quantity.*
import trading.quantity.algebra.*
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.gridQuantityAlgebra.given
import trading.quantity.algebra.refinedAdditive.given
import trading.quantity.refinement.*

object MalformedDimensionAlgebra:
  type Bad = Dim[Power["algebra:bad", 0] *: EmptyTuple]
  sealed trait G

  // OFFENDING-BEGIN
  val validity = summon[Normalize[Bad]]
  val vector = summon[VectorSpace[Quantity[Bad], Rational]]
  val module = summon[LeftModule[GridQuantity[Bad, G], BigInt]]
  val monoid = summon[AdditiveCommutativeMonoid[NonNegative[Quantity[Bad]]]]
  // OFFENDING-END

end MalformedDimensionAlgebra
