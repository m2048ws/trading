package external.reference.positive

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.quantity.Dim
import trading.quantity.Quantity
import trading.quantity.Rational
import trading.quantity.SameDimension
import trading.reference.*

object GenericReferenceDataClient:
  def exact[D <: Dim, G](handle: GridHandle.Grid[D, G], coordinate: BigInt): Quantity[D] =
    handle.asQuantity(handle.fromCoordinate(coordinate))

  def reconcileDimensions[A <: Dim, B <: Dim](
    left: DimensionHandle[A],
    right: DimensionHandle[B]
  ): Either[ReferenceDataError, SameDimension[A, B]] =
    DimensionHandle.reconcile(left, right)

  val amount = exact(grid, BigInt(42))
  val sameDimension = reconcileDimensions(asset.dimension, grid.dimension)

  assert(amount.coefficient == Rational(21, 50))
  assert(sameDimension.isRight)

end GenericReferenceDataClient
