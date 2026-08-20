package external.fixtures.positive

import algebra.ring.AdditiveCommutativeGroup

import trading.quantity.*
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.gridQuantityAlgebra.given
import trading.quantity.refinement.*

object GenericDimensionPreserving:
  def total[D <: Dimension](left: Quantity[D], right: Quantity[D])(using Normalize[D]): Quantity[D] =
    left + right

  def totalEquivalent[A <: Dimension, B <: Dimension](left: Quantity[A], right: Quantity[B])(using
    Normalize[A],
    SameDimension[B, A]
  ): Quantity[A] =
    left + right.alignTo[A]

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

  def crossGrid[D <: Dimension, G, H](
    left: GridQuantity[D, G],
    right: GridQuantity[D, H],
    leftGrid: GridRef.Grid[D, G],
    rightGrid: GridRef.Grid[D, H]
  )(using Normalize[D]): (Quantity[D], Quantity[D]) =
    (
      left.addExact(right, leftGrid, rightGrid),
      left.subtractExact(right, leftGrid, rightGrid)
    )

  def refined[D <: Dimension](
    left: NonNegative[Quantity[D]],
    right: NonNegative[Quantity[D]]
  )(using Normalize[D]
  ): (NonNegative[Quantity[D]], Quantity[D]) =
    (left.add(right), left.subtract(right))

  def algebra[D <: Dimension, G](using
    Normalize[D]
  ): (
    AdditiveCommutativeGroup[Quantity[D]],
    AdditiveCommutativeGroup[GridQuantity[D, G]]
  ) =
    (summon, summon)

end GenericDimensionPreserving
