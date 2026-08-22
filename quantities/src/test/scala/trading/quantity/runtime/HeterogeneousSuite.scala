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

    val left    = new ResolvedGridQuantity(dimension)(leftGrid)(leftGrid.fromCoordinate(100))
    val right   = new ResolvedGridQuantity(dimension)(rightGrid)(rightGrid.fromCoordinate(23))
    val result  = HeterogeneousQuantity.addSameGrid(left, right).toOption.get
    val doubled = result.value + result.value

    assertEquals(result.grid.coordinate(result.value), BigInt(123))
    assertEquals(result.grid.coordinate(doubled), BigInt(246))
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
    val doubled                      = exact.value * Rational(2)
    assertEquals(exact.value.coefficient, Rational(1, 25))
    assertEquals(doubled.coefficient, Rational(2, 25))

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

  test("heterogeneous multiplication retains the raw product witness"):
    val registry       = new QuantityRegistry
    val leftDimension  = registeredDimension(registry, "heterogeneous-product-left")
    val rightDimension = registeredDimension(registry, "heterogeneous-product-right")
    val leftGrid       = registeredGrid(registry, leftDimension, "heterogeneous-product-left-grid", Rational(1, 10))
    val rightGrid      = registeredGrid(registry, rightDimension, "heterogeneous-product-right-grid", Rational(1, 100))
    val left           = new ResolvedGridQuantity(leftDimension)(leftGrid)(leftGrid.fromCoordinate(2))
    val right          = new ResolvedGridQuantity(rightDimension)(rightGrid)(rightGrid.fromCoordinate(300))

    val result = HeterogeneousQuantity.multiplyExact(left, right)

    assertEquals(result.value.coefficient, Rational(3, 5))
    assertEquals(result.dimension.key, DimensionKey.multiply(leftDimension.dimension.key, rightDimension.dimension.key))

  test("raw grid products retain expression witnesses and accept checked runtime alignment"):
    val registry     = new QuantityRegistry
    val position     = registeredDimension(registry, "heterogeneous-normalized-position")
    val settlement   = registeredDimension(registry, "heterogeneous-normalized-settlement")
    val priceKey     = DimensionKey.multiply(settlement.dimension.key, DimensionKey.inverse(position.dimension.key))
    val price        = registry.registerDimension(priceKey).toOption.get
    val positionGrid = registeredGrid(registry, position, "heterogeneous-normalized-position-grid", Rational(1, 10))
    val amount       = positionGrid.fromCoordinate(2)
    val exactPrice   = Quantity(price.dimension.asDimensionRef, Rational(15))

    val product: Quantity[Times[position.D, price.D]] =
      amount.multiplyExact[price.D](exactPrice, positionGrid.asGridRef)
    val productDimension: DimRef[Times[position.D, price.D]] =
      DimRef.times(position.dimension.asDimensionRef, price.dimension.asDimensionRef)
    val checked = SameDimension.between(productDimension, settlement.dimension.asDimensionRef).get
    val aligned: Quantity[settlement.D] = product.alignTo[settlement.D](using checked)

    assertEquals(aligned.coefficient, Rational(3))

  test("checked runtime identity recovery supports homogeneous arithmetic"):
    val identity = AtomId("runtime:shared-identity")
    val left     = DimRef.atomic(identity)
    val right    = DimRef.atomic(identity)
    val checked  = SameDimension.between(right.dimension, left.dimension).get
    val first    = Quantity(left.dimension, Rational(2))
    val second   = Quantity(right.dimension, Rational(3)).alignTo[left.D](using checked)

    assertEquals((first + second).coefficient, Rational(5))

  test("runtime-resolved endpoints construct, apply, compose, reciprocate, and cross rates directly"):
    val registry                           = new QuantityRegistry
    val base                               = registeredDimension(registry, "runtime-rate-base")
    val quote                              = registeredDimension(registry, "runtime-rate-quote")
    val settlement                         = registeredDimension(registry, "runtime-rate-settlement")
    val baseToQuote: Rate[base.D, quote.D] =
      Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, Rational(60_000))
    val quoteToSettlement: Rate[quote.D, settlement.D] =
      Rate(quote.dimension.asDimensionRef, settlement.dimension.asDimensionRef, Rational(9, 10))
    val baseToSettlement: Rate[base.D, settlement.D] = baseToQuote.andThen(quoteToSettlement)
    val amount                                       = Quantity(base.dimension.asDimensionRef, Rational(1, 10))
    val reciprocal: Rate[settlement.D, base.D]       = NonZero(baseToSettlement).toOption.get.reciprocalRate
    val sharedTarget: Rate[quote.D, settlement.D]    = quoteToSettlement
    val crossDivisor                                 = NonZero(sharedTarget).toOption.get
    val quotePerBase: Rate[base.D, quote.D]          = baseToSettlement.crossRate(crossDivisor)

    assertEquals(amount.applyRate(baseToSettlement).coefficient, Rational(5_400))
    assertEquals(reciprocal.coefficient, Rational(1, 54_000))
    assertEquals(quotePerBase.coefficient, Rational(60_000))

  test("BitMEX-shaped adapters supply every endpoint explicitly at the quantity boundary"):
    val registry   = new QuantityRegistry
    val base       = registeredDimension(registry, "adapter:ETH")
    val quote      = registeredDimension(registry, "adapter:USD")
    val position   = registeredDimension(registry, "adapter:contracts")
    val settlement = registeredDimension(registry, "adapter:XBT")

    val quotePerBase       = Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, Rational(3_000))
    val basePerPosition    = Rate(position.dimension.asDimensionRef, base.dimension.asDimensionRef, Rational(1, 100))
    val settlementPerQuote =
      Rate(quote.dimension.asDimensionRef, settlement.dimension.asDimensionRef, Rational(1, 60_000))
    val settlementPerPosition = basePerPosition.andThen(quotePerBase).andThen(settlementPerQuote)
    val contracts             = Quantity(position.dimension.asDimensionRef, 10)

    assertEquals(contracts.applyRate(settlementPerPosition).coefficient, Rational(1, 200))

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
