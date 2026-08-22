package external

import trading.quantity.*

/** Real downstream compilation fixture for the public static-dimension derivation surface. */
object StaticDimensionDownstreamFixture:
  val left  = DimRef.atomic(AtomId("downstream-static-left"))
  val right = DimRef.atomic(AtomId("downstream-static-right"))

  type LeftRight   = Times[left.D, right.D]
  type LeftInverse = Inverse[left.D]

  val productRef: DimRef[LeftRight]               = DimRef.times(left.dimension, right.dimension)
  val inverseRef: DimRef[LeftInverse]             = DimRef.inverse(left.dimension)
  val quotientRef: DimRef[Divide[left.D, left.D]] = DimRef.divide(left.dimension, left.dimension)

  val product: Quantity[LeftRight] =
    Quantity(left.dimension, 2) * Quantity(right.dimension, 3)

  val commuted: SameDimension[Times[left.D, right.D], Times[right.D, left.D]] = summon

end StaticDimensionDownstreamFixture
