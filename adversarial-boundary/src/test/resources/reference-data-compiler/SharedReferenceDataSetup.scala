package external.reference.fixtures

import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

object SharedReferenceDataSetup:
  def validAssetId(value: String): AssetId =
    AssetId.from(value).fold(error => throw new AssertionError(error.toString), identity)

  def validGridId(value: String): GridId =
    GridId.from(value).fold(error => throw new AssertionError(error.toString), identity)

  def validGridVersion(value: Long): GridVersion =
    GridVersion.from(value).fold(error => throw new AssertionError(error.toString), identity)

  def assetDefinition(name: String): AssetDefinition =
    AssetDefinition(validAssetId(name), AtomId(s"reference:$name"))

  def gridDefinition(
    dimension: DimKey,
    name: String,
    quantum: Rational = Rational(1, 100)
  ): GridDefinition =
    GridDefinition(
      GridIdentity(dimension, GridKey(validGridId(name), validGridVersion(1))),
      PositiveRational(quantum).fold(error => throw new AssertionError(error.toString), identity)
    )

  val registry = new QuantityRegistry
  val asset = registry
    .registerAsset(assetDefinition("canonical-asset"))
    .fold(error => throw new AssertionError(error.toString), identity)
  val grid = registry
    .registerGrid(asset)(gridDefinition(asset.dimension.key, "canonical-grid"))
    .fold(error => throw new AssertionError(error.toString), identity)

end SharedReferenceDataSetup
