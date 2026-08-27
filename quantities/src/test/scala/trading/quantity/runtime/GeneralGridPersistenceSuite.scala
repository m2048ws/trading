package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class GeneralGridPersistenceSuite extends FunSuite:
  test("general packing persists normalized compound dimensions"):
    val usd =
      trading.quantity.testkit.TestAsset
        .runtime:
          AssetId:
            "packed-price-usd"
    val btc =
      trading.quantity.testkit.TestAsset
        .runtime:
          AssetId:
            "packed-price-btc"
    val priceDimension           = DimRef.divide(usd.dimension, btc.dimension)
    val registry                 = new QuantityRegistry
    val registeredPriceDimension = registry.registerDimension(priceDimension.key).toOption.get
    val priceGrid                =
      registry
        .registerGrid(registeredPriceDimension):
          GridDefinition(
            priceDimension.key,
            GridId:
              "packed-price-grid"
            ,
            GridVersion(3),
            PositiveRational.exact(1, 100).toOption.get
          )
        .toOption
        .get
    val packed =
      PackedGridQuantity.pack(priceGrid):
        priceGrid.fromCoordinate:
          6_000_001

    assertEquals(
      packed.dimension,
      DimKey:
        List(
          AtomId:
            "packed-price-usd"
          -> BigInt(1),
          AtomId:
            "packed-price-btc"
          -> BigInt(-1)
        )
    )
    assertEquals(
      packed.coordinate,
      BigInt:
        6_000_001
    )

  test("general decoding resolves normalized compound dimension before its grid"):
    val registry = new QuantityRegistry
    val priceKey =
      DimKey:
        List(
          AtomId:
            "decoded-usd"
          -> BigInt(1),
          AtomId:
            "decoded-btc"
          -> BigInt(-1)
        )
    val dimension =
      registry
        .registerDimension:
          priceKey
        .toOption
        .get
    val definition =
      GridDefinition(
        priceKey,
        GridId:
          "decoded-usd-per-btc-cent"
        ,
        GridVersion(2),
        PositiveRational.exact(1, 100).toOption.get
      )
    val grid =
      registry
        .registerGrid(dimension):
          definition
        .toOption
        .get
    val packed =
      PackedGridQuantity(
        priceKey,
        grid.id,
        grid.version,
        BigInt:
          6_000_001
      )
    val resolved = PackedGridQuantity.decode(packed, registry).toOption.get
    val moved    = resolved.value + resolved.value

    assertEquals(resolved.dimension.dimension.key, priceKey)
    assertEquals(
      resolved.grid
        .coordinate:
          resolved.value
      ,
      BigInt:
        6_000_001
    )
    assertEquals(resolved.grid.coordinate(moved), BigInt(12_000_002))

  test("general decoding scopes a repeated grid ID by canonical dimension") {
    val registry = new QuantityRegistry
    val usd      =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "scoped-grid-usd"
            ,
            AtomId:
              "asset:scoped-grid-usd"
          )
        .toOption
        .get
    val btc =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "scoped-grid-btc"
            ,
            AtomId:
              "asset:scoped-grid-btc"
          )
        .toOption
        .get
    val id =
      GridId:
        "shared-packed-grid"
    val version = GridVersion(1)
    val _       =
      registry
        .registerGrid(usd):
          GridDefinition(usd.dimension.key, id, version, PositiveRational.exact(1, 100).toOption.get)
        .toOption
        .get
    val _ =
      registry
        .registerGrid(btc):
          GridDefinition(btc.dimension.key, id, version, PositiveRational.exact(1, 100_000_000).toOption.get)
        .toOption
        .get

    val usdResolved =
      PackedGridQuantity
        .decode(
          PackedGridQuantity(
            usd.dimension.key,
            id,
            version,
            BigInt:
              100
          ),
          registry
        )
        .toOption
        .get
    val btcResolved =
      PackedGridQuantity
        .decode(
          PackedGridQuantity(
            btc.dimension.key,
            id,
            version,
            BigInt:
              100
          ),
          registry
        )
        .toOption
        .get

    assertEquals(usdResolved.dimension.dimension.key, usd.dimension.key)
    assertEquals(btcResolved.dimension.dimension.key, btc.dimension.key)
    assertEquals(
      usdResolved.grid
        .asQuantity:
          usdResolved.value
        .coefficient,
      Rational(1)
    )
    assertEquals(
      btcResolved.grid
        .asQuantity:
          btcResolved.value
        .coefficient,
      Rational(1, 1_000_000)
    )
  }

  test("general decoding rejects dimension-grid mismatches"):
    val registry = new QuantityRegistry
    val usd      =
      registry
        .registerDimension:
          DimKey.atom:
            AtomId:
              "mismatch-usd"
        .toOption
        .get
    val btc =
      registry
        .registerDimension:
          DimKey.atom:
            AtomId:
              "mismatch-btc"
        .toOption
        .get
    val definition =
      GridDefinition(
        btc.dimension.key,
        GridId:
          "mismatch-grid"
        ,
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val _ =
      registry
        .registerGrid(btc):
          definition
        .toOption
        .get
    val packed = PackedGridQuantity(usd.dimension.key, definition.id, definition.version, BigInt(1))

    assertEquals(
      PackedGridQuantity.decode(packed, registry),
      Left:
        PackedGridDimensionMismatch(usd.dimension.key, btc.dimension.key, definition.key)
    )

  test("unknown dimensions fail before any grid lookup"):
    val registry = new QuantityRegistry
    val known    =
      registry
        .registerDimension:
          DimKey.atom:
            AtomId:
              "known-dimension"
        .toOption
        .get
    val definition =
      GridDefinition(
        known.dimension.key,
        GridId:
          "known-grid"
        ,
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val _ =
      registry
        .registerGrid(known):
          definition
        .toOption
        .get
    val unknown =
      DimKey.atom:
        AtomId:
          "unknown-dimension"
    val packed = PackedGridQuantity(unknown, definition.id, definition.version, BigInt(1))

    assertEquals(
      PackedGridQuantity.decode(packed, registry),
      Left:
        UnknownDimension:
          unknown
    )

end GeneralGridPersistenceSuite
