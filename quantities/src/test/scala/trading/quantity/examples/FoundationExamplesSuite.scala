package trading.quantity.examples

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

class FoundationExamplesSuite extends FunSuite:

  private final class Fixture:
    val registry = new QuantityRegistry
    val usd      =
      registry
        .registerAsset:
          AssetDefinition(AssetId("USD"), AtomId("asset:USD"))
        .toOption
        .get
    val btc =
      registry
        .registerAsset:
          AssetDefinition(AssetId("BTC"), AtomId("asset:BTC"))
        .toOption
        .get
    val xbt =
      registry
        .registerAsset:
          AssetDefinition(AssetId("XBT"), AtomId("asset:XBT"))
        .toOption
        .get
    val cents =
      registry
        .registerGrid(usd):
          GridDefinition(
            usd.dimension.key,
            GridId("USD-cent"),
            GridVersion(1),
            PositiveRational.exact(1, 100).toOption.get
          )
        .toOption
        .get
    val alternateCents =
      registry
        .registerGrid(usd):
          GridDefinition(
            usd.dimension.key,
            GridId("USD-cent-alternate"),
            GridVersion(1),
            PositiveRational.exact(1, 100).toOption.get
          )
        .toOption
        .get
    val threeCents =
      registry
        .registerGrid(usd):
          GridDefinition(
            usd.dimension.key,
            GridId("USD-three-cent"),
            GridVersion(1),
            PositiveRational.exact(3, 100).toOption.get
          )
        .toOption
        .get
    val satoshis =
      registry
        .registerGrid(btc):
          GridDefinition(
            btc.dimension.key,
            GridId("BTC-satoshi"),
            GridVersion(1),
            PositiveRational.exact(1, 100_000_000).toOption.get
          )
        .toOption
        .get
    val xbtSatoshis =
      registry
        .registerGrid(xbt):
          GridDefinition(
            xbt.dimension.key,
            GridId("XBT-satoshi"),
            GridVersion(1),
            PositiveRational.exact(1, 100_000_000).toOption.get
          )
        .toOption
        .get

  end Fixture

  test("runtime assets and uniform grids retain exact mathematical values"):
    val fixture  = new Fixture
    val dollars  = fixture.cents.fromCoordinate(12_345)
    val bitcoin  = fixture.satoshis.fromCoordinate(10_000_000)
    val sixCents = fixture.threeCents.fromCoordinate(2)
    val oneCent  = Quantity(fixture.usd.dimension.asDimensionRef, Rational(1, 100))

    assertEquals(fixture.cents.asQuantity(dollars).coefficient, Rational(2469, 20))
    assertEquals(fixture.satoshis.asQuantity(bitcoin).coefficient, Rational(1, 10))
    assertEquals(fixture.threeCents.asQuantity(sixCents).coefficient, Rational(3, 50))
    assert(oneCent.narrowExactlyTo(fixture.threeCents.asGridRef).isLeft)

  test("0.1 BTC at 60000.01 USD/BTC produces exact 6000.001 USD before projection"):
    val fixture   = new Fixture
    val amount    = fixture.satoshis.fromCoordinate(10_000_000)
    val usdPerBtc =
      Rate(
        fixture.btc.dimension.asDimensionRef,
        fixture.usd.dimension.asDimensionRef,
        Rational(6_000_001, 100)
      )
    val notional: Quantity[fixture.usd.D] = amount.applyRate(usdPerBtc, fixture.satoshis.asGridRef)

    assertEquals(notional.coefficient, Rational(6_000_001, 1000))
    assert(notional.narrowExactlyTo(fixture.cents.asGridRef).isLeft)

    val quantized = notional.quantizeTo(fixture.cents.asGridRef, QuantizationPolicy.HalfEven)
    val projected = fixture.cents.asQuantity(quantized.value)

    assertEquals(fixture.cents.coordinate(quantized.value), BigInt(600_000))
    assertEquals(quantized.residual.coefficient, Rational(1, 1000))
    assertEquals(projected.coefficient + quantized.residual.coefficient, notional.coefficient)

    val allocation =
      fixture.cents
        .fromCoordinate(1000)
        .allocateEvenly(PositiveInt(3).toOption.get, RemainderOrder.FirstToLast, fixture.cents.asGridRef)
    assertEquals(allocation.parts.map(fixture.cents.coordinate), Vector(BigInt(334), BigInt(333), BigInt(333)))

  test("an inverse calculation may produce a valid exact XBT quantity that is off the satoshi grid"):
    val fixture                          = new Fixture
    val inverseCoefficient               = Rational.one./(Rational(100_001, 2)).toOption.get
    val inverse: Quantity[fixture.xbt.D] =
      Quantity(fixture.xbt.dimension.asDimensionRef, inverseCoefficient)

    assertEquals(inverse.coefficient, Rational(2, 100_001))
    assert(inverse.narrowExactlyTo(fixture.xbtSatoshis.asGridRef).isLeft)

    val projected = inverse.quantizeTo(fixture.xbtSatoshis.asGridRef, QuantizationPolicy.HalfEven)
    val embedded  = projected.value.asQuantity(fixture.xbtSatoshis.asGridRef)
    assertEquals(embedded.coefficient + projected.residual.coefficient, inverse.coefficient)

  test("registered mathematical quantities restore identity and distinct grids remain distinct"):
    val fixture    = new Fixture
    val coordinate = BigInt(2).pow(160) + 7
    val packed     =
      PackedAssetGridQuantity.pack(fixture.btc)(fixture.satoshis):
        fixture.satoshis.fromCoordinate(coordinate)
    val restored = PackedAssetGridQuantity.decode(packed, fixture.registry).toOption.get

    assertEquals(restored.asset.id, fixture.btc.id)
    assertEquals(restored.grid.key, fixture.satoshis.key)
    assertEquals(restored.grid.coordinate(restored.value), coordinate)

    val primary   = fixture.cents.fromCoordinate(100)
    val alternate = fixture.alternateCents.fromCoordinate(100)
    assert(primary.exactlyEquals(alternate, fixture.cents.asGridRef, fixture.alternateCents.asGridRef))
    assert(SameGrid.between(fixture.cents.asGridRef, fixture.alternateCents.asGridRef).isLeft)

end FoundationExamplesSuite
