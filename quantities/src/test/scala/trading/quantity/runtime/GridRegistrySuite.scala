package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class GridRegistrySuite extends FunSuite:
  test("grid registration interns immutable definitions within a canonical dimension"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "grid-usd"
            ,
            AtomId:
              "asset:grid-usd"
          )
        .toOption
        .get
    val definition =
      GridDefinition(
        asset.dimension.key,
        GridId:
          "grid-usd-cent"
        ,
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val first =
      registry
        .registerGrid(asset):
          definition
        .toOption
        .get
    val second =
      registry
        .registerGrid(asset):
          definition
        .toOption
        .get
    val resolved =
      registry
        .resolveGrid(asset):
          definition.key
        .toOption
        .get

    assert:
      first
        .asInstanceOf[AnyRef]
        .eq:
          second.asInstanceOf[AnyRef]
    assert:
      first
        .asInstanceOf[AnyRef]
        .eq:
          resolved.asInstanceOf[AnyRef]
    val value: GridQuantity[asset.D, first.G] =
      first.fromCoordinate:
        1000
    assertEquals(
      first
        .asQuantity:
          value
        .coefficient,
      Rational:
        10
    )
    assertEquals(registry.registeredGridCount, 1)

  test("conflicting grid definitions at one immutable key are rejected"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "grid-conflict"
            ,
            AtomId:
              "asset:grid-conflict"
          )
        .toOption
        .get
    val key =
      GridKey(
        GridId:
          "conflicting-grid"
        ,
        GridVersion(1)
      )
    val first = GridDefinition(asset.dimension.key, key.id, key.version, PositiveRational.exact(1, 100).toOption.get)
    val conflicting = first.copy(quantum = PositiveRational.exact(3, 100).toOption.get)

    assert:
      registry
        .registerGrid(asset):
          first
        .isRight
    assertEquals(
      registry.registerGrid(asset):
        conflicting
      ,
      Left:
        ConflictingGridDefinition(asset.dimension.key, key, Rational(1, 100), Rational(3, 100))
    )

  test("canonical dimension scopes the same grid ID and version") {
    val registry = new QuantityRegistry
    val usd      =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "grid-key-usd"
            ,
            AtomId:
              "asset:grid-key-usd"
          )
        .toOption
        .get
    val btc =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "grid-key-btc"
            ,
            AtomId:
              "asset:grid-key-btc"
          )
        .toOption
        .get
    val id =
      GridId:
        "shared-grid-name"
    val version = GridVersion(1)
    val key     = GridKey(id, version)
    val usdGrid =
      registry
        .registerGrid(usd):
          GridDefinition(usd.dimension.key, id, version, PositiveRational.exact(1, 100).toOption.get)
        .toOption
        .get
    val btcGrid =
      registry
        .registerGrid(btc):
          GridDefinition(btc.dimension.key, id, version, PositiveRational.exact(1, 100_000_000).toOption.get)
        .toOption
        .get
    val resolvedUsd =
      registry
        .resolveGrid(usd):
          key
        .toOption
        .get
    val resolvedBtc =
      registry
        .resolveGrid(btc):
          key
        .toOption
        .get

    assertEquals(registry.registeredGridCount, 2)
    assert:
      resolvedUsd
        .asInstanceOf[AnyRef]
        .eq:
          usdGrid.asInstanceOf[AnyRef]
    assert:
      resolvedBtc
        .asInstanceOf[AnyRef]
        .eq:
          btcGrid.asInstanceOf[AnyRef]
    assertEquals(
      resolvedUsd
        .asQuantity:
          resolvedUsd.fromCoordinate(1)
        .coefficient,
      Rational(1, 100)
    )
    assertEquals(
      resolvedBtc
        .asQuantity:
          resolvedBtc.fromCoordinate(1)
        .coefficient,
      Rational(1, 100_000_000)
    )
  }

  test("a grid definition cannot claim a different dimension"):
    val registry = new QuantityRegistry
    val usd      =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "grid-dimension-usd"
            ,
            AtomId:
              "asset:grid-dimension-usd"
          )
        .toOption
        .get
    val btc =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "grid-dimension-btc"
            ,
            AtomId:
              "asset:grid-dimension-btc"
          )
        .toOption
        .get
    val definition =
      GridDefinition(
        btc.dimension.key,
        GridId:
          "mismatched-grid"
        ,
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )

    assertEquals(
      registry.registerGrid(usd):
        definition
      ,
      Left:
        trading.quantity.runtime.GridDimensionMismatch(usd.dimension.key, btc.dimension.key)
    )

end GridRegistrySuite
