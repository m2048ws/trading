package external.reference.positive

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.quantity.Rational
import trading.reference.*

object ConcreteReferenceDataClient:
  val sameAsset = registry.resolveAsset(asset.id)
  val sameGrid  = registry.resolveGrid(asset.dimension)(grid.key)
  val value     = grid.fromCoordinate(BigInt(125))

  assert(sameAsset.contains(asset))
  assert(sameGrid.contains(grid))
  assert(grid.coordinate(value) == BigInt(125))
  assert(grid.asQuantity(value).coefficient == Rational(5, 4))
  assert(AssetId.from(" ") == Left(EmptyAssetId))
  assert(GridId.from("") == Left(EmptyGridId))
  assert(GridVersion.from(0) == Left(NonPositiveGridVersion(0)))

end ConcreteReferenceDataClient
