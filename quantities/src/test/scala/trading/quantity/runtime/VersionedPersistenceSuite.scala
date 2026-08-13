package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class VersionedPersistenceSuite extends FunSuite:
  test("decoding rejects unknown grid versions without reinterpretation"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "versioned-usd"
            ,
            AtomId:
              "asset:versioned-usd"
          )
        .toOption
        .get
    val definition =
      GridDefinition(
        asset.dimension.key,
        GridId:
          "versioned-usd-cent"
        ,
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val _ =
      registry
        .registerGrid(asset):
          definition
        .toOption
        .get
    val packed =
      PackedAssetGridQuantity(
        asset.id,
        asset.dimension.key,
        definition.id,
        GridVersion(2),
        BigInt:
          1000
      )

    assertEquals(
      PackedAssetGridQuantity.decode(packed, registry),
      Left:
        UnknownGrid(asset.dimension.key, GridKey(definition.id, GridVersion(2)))
    )

  test("packed asset quantities round-trip through a fresh registry after restart") {
    val assetDefinition =
      AssetDefinition(
        AssetId:
          "restart-usd"
        ,
        AtomId:
          "asset:restart-usd"
      )
    val gridId =
      GridId:
        "restart-usd-cent"
    val gridVersion = GridVersion(1)
    val quantum     = PositiveRational.exact(1, 100).toOption.get

    val writerRegistry = new QuantityRegistry
    val writerAsset    =
      writerRegistry
        .registerAsset:
          assetDefinition
        .toOption
        .get
    val gridDefinition = GridDefinition(writerAsset.dimension.key, gridId, gridVersion, quantum)
    val writerGrid     =
      writerRegistry
        .registerGrid(writerAsset):
          gridDefinition
        .toOption
        .get
    val coordinate =
      BigInt(2).pow:
        200
      + 17
    val packed =
      PackedAssetGridQuantity.pack(writerAsset)(writerGrid):
        writerGrid.fromCoordinate:
          coordinate

    val readerRegistry = new QuantityRegistry
    val readerAsset    =
      readerRegistry
        .registerAsset:
          assetDefinition
        .toOption
        .get
    val readerDefinition = gridDefinition.copy(dimension = readerAsset.dimension.key)
    val _                =
      readerRegistry
        .registerGrid(readerAsset):
          readerDefinition
        .toOption
        .get
    val restored = PackedAssetGridQuantity.decode(packed, readerRegistry).toOption.get

    assert:
      !writerAsset
        .asInstanceOf[AnyRef]
        .eq:
          readerAsset.asInstanceOf[AnyRef]
    assertEquals(
      restored.grid
        .coordinate:
          restored.value
      ,
      coordinate
    )
    assertEquals(restored.grid.key, writerGrid.key)
  }

  test("historical grid versions remain distinct and decode using the recorded version"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "historical-usd"
            ,
            AtomId:
              "asset:historical-usd"
          )
        .toOption
        .get
    val gridId =
      GridId:
        "historical-usd-grid"
    val versionOne =
      GridDefinition(asset.dimension.key, gridId, GridVersion(1), PositiveRational.exact(1, 100).toOption.get)
    val versionTwo =
      GridDefinition(asset.dimension.key, gridId, GridVersion(2), PositiveRational.exact(3, 100).toOption.get)
    val oldGrid =
      registry
        .registerGrid(asset):
          versionOne
        .toOption
        .get
    val currentGrid =
      registry
        .registerGrid(asset):
          versionTwo
        .toOption
        .get
    val packedHistorical = PackedAssetGridQuantity(asset.id, asset.dimension.key, gridId, GridVersion(1), BigInt(6))
    val restored         = PackedAssetGridQuantity.decode(packedHistorical, registry).toOption.get

    assertEquals(restored.grid.version, GridVersion(1))
    assertEquals(
      restored.grid
        .asQuantity:
          restored.value
        .coefficient,
      Rational(3, 50)
    )
    assertEquals(
      currentGrid
        .asQuantity:
          currentGrid.fromCoordinate(6)
        .coefficient,
      Rational(9, 50)
    )
    assertEquals(oldGrid.key, restored.grid.key)

end VersionedPersistenceSuite
