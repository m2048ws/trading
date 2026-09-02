package external.codec.positive

import trading.codec.*
import trading.quantity.Dim
import trading.quantity.GridQuantity
import trading.reference.Asset
import trading.reference.GridHandle

object GridCoordinateRecordClient:
  def packGeneral[D <: Dim](
    grid: GridHandle[D]
  )(
    value: GridQuantity[D, grid.G]
  ): GeneralGridCoordinateRecord.V1 =
    GeneralGridCoordinateRecord.pack(grid)(value)

  def packAsset(
    asset: Asset
  )(
    grid: GridHandle[asset.D]
  )(
    value: GridQuantity[asset.D, grid.G]
  ): AssetGridCoordinateRecord.V1 =
    AssetGridCoordinateRecord.pack(asset)(grid)(value)

  def generalCoordinate(value: DecodedGridQuantity): BigInt =
    value.grid.coordinate(value.value)

  def assetCoordinate(value: DecodedAssetGridQuantity): BigInt =
    value.grid.coordinate(value.value)

  val generalResult
    : Either[GridCoordinateReconstructionFailure, DecodedGridQuantity] => Option[BigInt] =
    _.toOption.map(generalCoordinate)

  val indexedFailure: IndexedGridCoordinateReconstructionFailure => Int =
    _.recordIndex
end GridCoordinateRecordClient
