package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class HeterogeneousSuite extends FunSuite:

  private def registeredDimension(r: QuantityRegistry, atom: String): DimensionWitness =
    r
      .registerDimension(DimensionKey.atom(AtomId(atom)))
      .toOption
      .get

  private def registeredGrid(
    r: QuantityRegistry,
    d: DimensionWitness,
    id: String,
    q: Rational
  ): RegisteredGridRef[d.D] =
    r
      .registerGrid(d):
        GridDefinition(
          d.dimension.key,
          GridId(id),
          GridVersion(1),
          PositiveRational(q).toOption.get
        )
      .toOption
      .get

  test("heterogeneous same-grid addition recovers identity evidence first"):
    val registry  = new QuantityRegistry
    val dimension = registeredDimension(registry, "heterogeneous-same-grid-usd")
    val leftGrid  = registeredGrid(registry, dimension, "heterogeneous-cent", Rational(1, 100))
    val rightGrid =
      registry
        .resolveGrid(dimension)(leftGrid.key)
        .toOption
        .get

    val left   = new ResolvedGridQuantity(dimension)(leftGrid)(leftGrid.fromCoordinate(100))
    val right  = new ResolvedGridQuantity(dimension)(rightGrid)(rightGrid.fromCoordinate(23))
    val result = HeterogeneousQuantity.addSameGrid(left, right).toOption.get

    assertEquals(result.grid.coordinate(result.value), BigInt(123))
    assertEquals(result.grid.key, leftGrid.key)

  test("heterogeneous cross-grid addition recovers dimension evidence and returns an exact quantity"):
    val registry   = new QuantityRegistry
    val dimension  = registeredDimension(registry, "heterogeneous-cross-grid-usd")
    val cents      = registeredGrid(registry, dimension, "heterogeneous-cross-cent", Rational(1, 100))
    val threeCents = registeredGrid(registry, dimension, "heterogeneous-cross-three-cent", Rational(3, 100))
    val left       = new ResolvedGridQuantity(dimension)(cents)(cents.fromCoordinate(1))
    val right      = new ResolvedGridQuantity(dimension)(threeCents)(threeCents.fromCoordinate(1))

    assert(HeterogeneousQuantity.addSameGrid(left, right).isLeft)

    val exact: ResolvedExactQuantity = HeterogeneousQuantity.addExact(left, right).toOption.get
    assertEquals(exact.value.coefficient, Rational(1, 25))

  test("heterogeneous exact addition rejects incompatible dimensions"):
    val registry = new QuantityRegistry
    val usd      = registeredDimension(registry, "heterogeneous-usd")
    val btc      = registeredDimension(registry, "heterogeneous-btc")
    val usdGrid  = registeredGrid(registry, usd, "heterogeneous-usd-grid", Rational(1, 100))
    val btcGrid  = registeredGrid(registry, btc, "heterogeneous-btc-grid", Rational(1, 100000000))
    val left     = new ResolvedGridQuantity(usd)(usdGrid)(usdGrid.fromCoordinate(1))
    val right    = new ResolvedGridQuantity(btc)(btcGrid)(btcGrid.fromCoordinate(1))

    val result = HeterogeneousQuantity.addExact(left, right)
    assert:
      result.isLeft

  test("asset-specialized resolved quantities can join general heterogeneous handling"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(
            AssetId:
              "heterogeneous-asset"
            ,
            AtomId:
              "asset:heterogeneous"
          )
        .toOption
        .get
    val grid =
      registry
        .registerGrid(asset):
          GridDefinition(
            asset.dimension.key,
            GridId("heterogeneous-asset-grid"),
            GridVersion(1),
            PositiveRational.exact(1, 100).toOption.get
          )
        .toOption
        .get

    val packed   = PackedAssetGridQuantity(asset.id, asset.dimension.key, grid.id, grid.version, BigInt(7))
    val resolved = PackedAssetGridQuantity.decode(packed, registry).toOption.get
    val general  = HeterogeneousQuantity.generalize(resolved)

    assertEquals(
      general.grid
        .coordinate:
          general.value
      ,
      BigInt(7)
    )

end HeterogeneousSuite
