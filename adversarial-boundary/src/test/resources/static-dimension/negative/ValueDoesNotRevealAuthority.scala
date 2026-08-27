package external.fixtures.negative

import trading.quantity.*
import trading.quantity.runtime.*

object ValueDoesNotRevealAuthority:
  def observe[D <: Dim, G](quantity: Quantity[D], gridQuantity: GridQuantity[D, G]): Rational =
    quantity.coefficient

  // OFFENDING-BEGIN
  def dimensionRef[D <: Dim](value: Quantity[D]): DimRef[D]                                       = summon
  def runtimeKey[D <: Dim](value: Quantity[D]): DimKey                                      = value.key
  def gridRef[D <: Dim, G](value: GridQuantity[D, G]): GridRef.Grid[D, G]                         = summon
  def registeredDimension[D <: Dim](value: Quantity[D]): RegisteredDimensionRef[D]                = summon
  def registeredGrid[D <: Dim, G0](value: GridQuantity[D, G0]): RegisteredGridRef[D] { type G = G0 } = summon
  // OFFENDING-END

end ValueDoesNotRevealAuthority
