package external.fixtures.positive

import algebra.ring.AdditiveCommutativeGroup
import algebra.ring.AdditiveCommutativeSemigroup

import trading.quantity.*
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.gridQuantityAlgebra.given
import trading.quantity.algebra.refinedAdditive.given
import trading.quantity.grid.*
import trading.quantity.refinement.*

object GenericDimensionPreserving:
  def total[D <: Dim](left: Quantity[D], right: Quantity[D]): Quantity[D] =
    left + right

  def totalEquivalent[A <: Dim, B <: Dim](
    left: Quantity[A],
    right: Quantity[B]
  )(using SameDimension[B, A]
  ): Quantity[A] =
    left + right.alignTo[A]

  def reduce[D <: Dim](head: Quantity[D], tail: List[Quantity[D]]): Quantity[D] =
    tail.foldLeft(head)(_ + _)

  def exact[D <: Dim](
    left: Quantity[D],
    right: Quantity[D],
    divisor: NonZeroWhole
  ): (Quantity[D], Quantity[D], Quantity[D], Quantity[D]) =
    (
      left + right,
      left - right,
      left * Rational(2),
      left.exactDivideBy(divisor)
    )

  def grid[D <: Dim, G](
    left: GridQuantity[D, G],
    right: GridQuantity[D, G]
  ): (GridQuantity[D, G], GridQuantity[D, G], GridQuantity[D, G], GridQuantity[D, G]) =
    (
      left + right,
      left - right,
      left * BigInt(2),
      -left
    )

  def crossGrid[D <: Dim, G, H](
    left: GridQuantity[D, G],
    right: GridQuantity[D, H],
    leftGrid: GridRef.Grid[D, G],
    rightGrid: GridRef.Grid[D, H]
  ): (Quantity[D], Quantity[D]) =
    (
      left.addExact(right, leftGrid, rightGrid),
      left.subtractExact(right, leftGrid, rightGrid)
    )

  def refined[D <: Dim](
    left: NonNegative[Quantity[D]],
    right: NonNegative[Quantity[D]]
  ): (NonNegative[Quantity[D]], Quantity[D]) =
    (left.add(right), left.subtract(right))

  def project[D <: Dim](value: Quantity[D], target: GridRef[D]) =
    value.narrowExactlyTo(target)

  def quantize[D <: Dim](value: Quantity[D], target: GridRef[D], policy: QuantizationPolicy) =
    value.quantizeTo(target, policy)

  def allocate[D <: Dim, G](
    value: GridQuantity[D, G],
    count: PositiveInt,
    grid: GridRef.Grid[D, G]
  ) =
    value.allocateEvenly(count, RemainderOrder.FirstToLast, grid)

  def positiveSemigroups[
    D <: Dim,
    G
  ]: (
    AdditiveCommutativeSemigroup[Positive[Quantity[D]]],
    AdditiveCommutativeSemigroup[Positive[GridQuantity[D, G]]]
  ) =
    (summon, summon)

  def zeros[D <: Dim, G](using DimRef[D]): (Quantity[D], GridQuantity[D, G]) =
    (Quantity.zero[D], GridQuantity.zero[D, G])

  def algebra[D <: Dim, G](
    using DimRef[D]
  ): (
    AdditiveCommutativeGroup[Quantity[D]],
    AdditiveCommutativeGroup[GridQuantity[D, G]]
  ) =
    (summon, summon)

end GenericDimensionPreserving
