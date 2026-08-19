package external.fixtures.negative

import trading.quantity.*

object DimRefDoesNotNormalize:
  def runtimeIdentity[D <: Dimension](dimension: DimRef[D]): DimensionKey =
    dimension.key

  // OFFENDING-BEGIN
  def zero[D <: Dimension](dimension: DimRef[D]): Quantity[D] =
    Quantity.zero[D]

  def add[D <: Dimension](dimension: DimRef[D], left: Quantity[D], right: Quantity[D]): Quantity[D] =
    left + right

  def scale[D <: Dimension](dimension: DimRef[D], value: Quantity[D]): Quantity[D] =
    value * Rational(2)
  // OFFENDING-END

end DimRefDoesNotNormalize
