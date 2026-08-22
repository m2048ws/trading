package external.fixtures.negative

import trading.quantity.*

object ExplicitDimRefIsNotAmbient:
  def runtimeIdentity[D <: Dimension](dimension: DimRef[D]): DimensionKey =
    dimension.key

  // OFFENDING-BEGIN
  def zero[D <: Dimension](dimension: DimRef[D]): Quantity[D] =
    Quantity.zero[D]
  // OFFENDING-END

end ExplicitDimRefIsNotAmbient
