package external.reference.positive

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.quantity.Dim
import trading.quantity.GridQuantity
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

  def retypeStableCoordinate[A <: Dim, G, B <: Dim, H](
    left: GridHandle.Grid[A, G],
    right: GridHandle.Grid[B, H],
    value: GridQuantity[A, G]
  ): Either[ReferenceDataError, GridQuantity[B, H]] =
    GridHandle.reconcile(left, right).map(_.retype(value))

  val amount = exact(grid, BigInt(42))
  val sameDimension = reconcileDimensions(asset.dimension, grid.dimension)
  val stableCoordinate = retypeStableCoordinate(grid, grid, grid.fromCoordinate(BigInt(42))).toOption.get

  assert(amount.coefficient == Rational(21, 50))
  assert(sameDimension.isRight)
  assert(grid.coordinate(stableCoordinate) == BigInt(42))

end GenericReferenceDataClient
