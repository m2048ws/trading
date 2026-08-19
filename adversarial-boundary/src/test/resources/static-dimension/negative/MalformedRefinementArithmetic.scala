package external.fixtures.negative

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

object MalformedRefinementArithmetic:
  type Bad = Dim[Power["refined:bad", 0] *: EmptyTuple]
  sealed trait G

  val divisor: PositiveWhole = PositiveWhole(2).toOption.get
  val count: PositiveInt = PositiveInt(2).toOption.get

  // OFFENDING-BEGIN
  val quantityZero: NonNegative[Quantity[Bad]] = NonNegative.quantityZero[Bad]
  val gridZero: NonNegative[GridQuantity[Bad, G]] = NonNegative.gridQuantityZero[Bad, G]

  def quantityAdd(left: NonNegative[Quantity[Bad]], right: NonNegative[Quantity[Bad]]) = left.add(right)
  def gridAdd(left: NonNegative[GridQuantity[Bad, G]], right: NonNegative[GridQuantity[Bad, G]]) = left.add(right)
  def quantitySubtract(left: NonNegative[Quantity[Bad]], right: NonNegative[Quantity[Bad]]) = left.subtract(right)
  def gridQuotRem(value: NonNegative[GridQuantity[Bad, G]], grid: GridRef.Grid[Bad, G]) =
    value.quotRemBy(divisor, grid)
  def allocate(value: GridQuantity[Bad, G], grid: GridRef.Grid[Bad, G]) =
    value.allocateEvenly(count, RemainderOrder.FirstToLast, grid)
  // OFFENDING-END

end MalformedRefinementArithmetic
