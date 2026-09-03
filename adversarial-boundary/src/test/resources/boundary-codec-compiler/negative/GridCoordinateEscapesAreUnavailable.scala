package external.codec.negative

import trading.codec.AssetGridCoordinateRecord
import trading.codec.GeneralGridCoordinateRecord
import trading.quantity.Dim
import trading.quantity.GridQuantity
import trading.quantity.Quantity
import trading.reference.Asset
import trading.reference.GridHandle

object GridCoordinateEscapesAreUnavailable:
  def retained[D <: Dim](
    grid: GridHandle[D]
  )(
    value: GridQuantity[D, grid.G]
  ): GeneralGridCoordinateRecord.V1 =
    GeneralGridCoordinateRecord.pack(grid)(value)

  // OFFENDING-BEGIN
  def arbitraryQuantity[D <: Dim](grid: GridHandle[D], value: Quantity[D]) =
    GeneralGridCoordinateRecord.pack(grid)(value)

  def arbitraryAssetQuantity(asset: Asset)(grid: GridHandle[asset.D])(value: Quantity[asset.D]) =
    AssetGridCoordinateRecord.pack(asset)(grid)(value)

  def crossGrid[D <: Dim](left: GridHandle[D], right: GridHandle[D])(
    value: GridQuantity[D, right.G]
  ) =
    GeneralGridCoordinateRecord.pack(left)(value)

  val packed: Class[trading.codec.PackedGridQuantity] = classOf[trading.codec.PackedGridQuantity]
  val packedAsset: Class[trading.codec.PackedAssetGridQuantity] =
    classOf[trading.codec.PackedAssetGridQuantity]
  val resolved: Class[trading.codec.ResolvedGridQuantity] = classOf[trading.codec.ResolvedGridQuantity]
  val resolvedAsset: Class[trading.codec.ResolvedAssetGridQuantity] =
    classOf[trading.codec.ResolvedAssetGridQuantity]
  val registry = trading.codec.QuantityRegistry
  // OFFENDING-END
end GridCoordinateEscapesAreUnavailable
