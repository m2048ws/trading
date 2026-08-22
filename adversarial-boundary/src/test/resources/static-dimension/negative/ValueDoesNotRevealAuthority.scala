package external.fixtures.negative

import trading.quantity.*
import trading.quantity.runtime.*

object ValueDoesNotRevealAuthority:
  def observe[D <: Dimension, G](quantity: Quantity[D], gridQuantity: GridQuantity[D, G]): Rational =
    quantity.coefficient

  // OFFENDING-BEGIN
  def dimensionRef[D <: Dimension](value: Quantity[D]): DimRef[D]                                       = summon
  def runtimeKey[D <: Dimension](value: Quantity[D]): DimensionKey                                      = value.key
  def gridRef[D <: Dimension, G](value: GridQuantity[D, G]): GridRef.Grid[D, G]                         = summon
  def registeredDimension[D <: Dimension](value: Quantity[D]): RegisteredDimensionRef[D]                = summon
  def registeredGrid[D <: Dimension, G0](value: GridQuantity[D, G0]): RegisteredGridRef[D] { type G = G0 } = summon
  // OFFENDING-END

end ValueDoesNotRevealAuthority
