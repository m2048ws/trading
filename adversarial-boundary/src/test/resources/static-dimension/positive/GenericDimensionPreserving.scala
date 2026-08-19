package external.fixtures.positive

import trading.quantity.*
import trading.quantity.refinement.*

object GenericDimensionPreserving:
  def exact[D <: Dimension](left: Quantity[D], right: Quantity[D], divisor: NonZeroWhole)(using
    Normalize[D]
  ): (Quantity[D], Quantity[D], Quantity[D], Quantity[D], Quantity[D]) =
    (
      Quantity.zero[D],
      left + right,
      left - right,
      left * Rational(2),
      left.exactDivideBy(divisor)
    )

  def grid[D <: Dimension, G](left: GridQuantity[D, G], right: GridQuantity[D, G])(using
    Normalize[D]
  ): (GridQuantity[D, G], GridQuantity[D, G], GridQuantity[D, G], GridQuantity[D, G]) =
    (
      GridQuantity.zero[D, G],
      left + right,
      left - right,
      -left
    )

  def refined[D <: Dimension](
    left: NonNegative[Quantity[D]],
    right: NonNegative[Quantity[D]]
  )(using Normalize[D]
  ): (NonNegative[Quantity[D]], Quantity[D]) =
    (left.add(right), left.subtract(right))

end GenericDimensionPreserving
