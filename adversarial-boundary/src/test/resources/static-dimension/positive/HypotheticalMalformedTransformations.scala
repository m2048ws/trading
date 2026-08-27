package external.fixtures.positive

import algebra.ring.AdditiveCommutativeSemigroup

import trading.quantity.*
import trading.quantity.algebra.refinedAdditive.given
import trading.quantity.grid.*
import trading.quantity.refinement.*

/** Malformed indices remain uninhabitable, but transformations over hypothetical parameters are proof-free. */
object HypotheticalMalformedTransformations:
  type Bad = Canonical[Power["hypothetical:bad", 0] *: EmptyTuple]
  sealed trait G
  sealed trait H

  def quantity(
    left: Quantity[Bad],
    right: Quantity[Bad],
    divisor: NonZeroWhole
  ): (Quantity[Bad], Quantity[Bad], Quantity[Bad], Quantity[Bad]) =
    (left + right, left - right, left * Rational(2), left.exactDivideBy(divisor))

  def grid(
    left: GridQuantity[Bad, G],
    right: GridQuantity[Bad, G]
  ): (GridQuantity[Bad, G], GridQuantity[Bad, G], GridQuantity[Bad, G], GridQuantity[Bad, G]) =
    (left + right, left - right, left * BigInt(2), -left)

  def crossGrid(
    left: GridQuantity[Bad, G],
    right: GridQuantity[Bad, H],
    leftGrid: GridRef.Grid[Bad, G],
    rightGrid: GridRef.Grid[Bad, H]
  ): (Quantity[Bad], Quantity[Bad]) =
    (left.addExact(right, leftGrid, rightGrid), left.subtractExact(right, leftGrid, rightGrid))

  def gridServices(
    value: Quantity[Bad],
    source: GridQuantity[Bad, G],
    sourceGrid: GridRef.Grid[Bad, G],
    target: GridRef[Bad],
    divisor: PositiveWhole,
    count: PositiveInt
  ) =
    (
      value.narrowExactlyTo(target),
      value.quantizeTo(target, QuantizationPolicy.HalfEven),
      source.quantizeTo(sourceGrid, target, QuantizationPolicy.HalfEven),
      source.quotRemBy(divisor, sourceGrid),
      source.allocateEvenly(count, RemainderOrder.FirstToLast, sourceGrid)
    )

  def refinement(
    left: NonNegative[Quantity[Bad]],
    right: NonNegative[Quantity[Bad]]
  ): (NonNegative[Quantity[Bad]], Quantity[Bad]) =
    (left.add(right), left.subtract(right))

  val positiveSemigroup: AdditiveCommutativeSemigroup[Positive[Quantity[Bad]]] = summon

end HypotheticalMalformedTransformations
