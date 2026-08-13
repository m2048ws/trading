package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class AssetGridPersistenceSuite extends FunSuite:
  test("asset quantities pack stable identity, exact version, and BigInt coordinate"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "packed-usd"
            ,
            AtomId:
              "asset:packed-usd"
          )
        .toOption
        .get
    val definition =
      GridDefinition(
        asset.dimension.key,
        GridId:
          "packed-usd-cent"
        ,
        GridVersion(7),
        PositiveRational.exact(1, 100).toOption.get
      )
    val grid =
      registry
        .registerGrid(asset):
          definition
        .toOption
        .get
    val coordinate =
      BigInt(2).pow:
        256
      + 123
    val packed =
      PackedAssetGridQuantity.pack(asset)(grid):
        grid.fromCoordinate:
          coordinate

    assertEquals(packed, PackedAssetGridQuantity(asset.id, asset.dimension.key, grid.id, grid.version, coordinate))

  test("asset decoding resolves asset before exact grid version and constructs a dependent value"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "decoded-usd"
            ,
            AtomId:
              "asset:decoded-usd"
          )
        .toOption
        .get
    val definition =
      GridDefinition(
        asset.dimension.key,
        GridId:
          "decoded-usd-cent"
        ,
        GridVersion(4),
        PositiveRational.exact(1, 100).toOption.get
      )
    val grid =
      registry
        .registerGrid(asset):
          definition
        .toOption
        .get
    val packed =
      PackedAssetGridQuantity(
        asset.id,
        asset.dimension.key,
        grid.id,
        grid.version,
        BigInt:
          1000
      )
    val resolved = PackedAssetGridQuantity.decode(packed, registry).toOption.get

    assertEquals(resolved.asset.id, asset.id)
    assertEquals(resolved.grid.key, definition.key)
    assertEquals(
      resolved.grid
        .asQuantity:
          resolved.value
        .coefficient,
      Rational:
        10
    )

  test("asset decoding rejects a grid registered to another asset"):
    val registry = new QuantityRegistry
    val usd      =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "packed-mismatch-usd"
            ,
            AtomId:
              "asset:packed-mismatch-usd"
          )
        .toOption
        .get
    val btc =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "packed-mismatch-btc"
            ,
            AtomId:
              "asset:packed-mismatch-btc"
          )
        .toOption
        .get
    val definition =
      GridDefinition(
        btc.dimension.key,
        GridId:
          "packed-mismatch-btc-grid"
        ,
        GridVersion(1),
        PositiveRational.exact(1, 100_000_000).toOption.get
      )
    val _ =
      registry
        .registerGrid(btc):
          definition
        .toOption
        .get
    val packed = PackedAssetGridQuantity(usd.id, usd.dimension.key, definition.id, definition.version, BigInt(1))

    assertEquals(
      PackedAssetGridQuantity.decode(packed, registry),
      Left:
        PackedGridDimensionMismatch(usd.dimension.key, btc.dimension.key, definition.key)
    )

end AssetGridPersistenceSuite
