package external.fixtures.negative

import trading.quantity.*

object ExplicitDimRefIsNotAmbient:
  def runtimeIdentity[D <: Dim](dimension: DimRef[D]): DimKey =
    dimension.key

  // OFFENDING-BEGIN
  def zero[D <: Dim](dimension: DimRef[D]): Quantity[D] =
    Quantity.zero[D]
  // OFFENDING-END

end ExplicitDimRefIsNotAmbient
