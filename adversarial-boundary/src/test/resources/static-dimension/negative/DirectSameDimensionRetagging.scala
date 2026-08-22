package external.fixtures.negative

import trading.quantity.*

object DirectSameDimensionRetagging:
  def quantity[A <: Dimension, B <: Dimension](
    same: SameDimension[A, B],
    value: Quantity[A]
  ): Unit =
    val _ = same
    val _ = value
    // OFFENDING-BEGIN
    val rejected: Quantity[B] = same.coerceQuantity(value)
    // OFFENDING-END

  def grid[A <: Dimension, B <: Dimension, G](
    same: SameDimension[A, B],
    value: GridQuantity[A, G]
  ): Unit =
    val _ = same
    val _ = value
    // OFFENDING-BEGIN
    val rejected: GridQuantity[B, G] = same.coerceGrid(value)
    // OFFENDING-END

end DirectSameDimensionRetagging
