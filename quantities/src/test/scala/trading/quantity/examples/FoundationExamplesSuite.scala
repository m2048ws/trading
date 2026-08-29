package trading.quantity.examples

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.testkit.TestAsset

class FoundationExamplesSuite extends FunSuite:

  private final class Fixture:
    val usd            = TestAsset.runtime(AtomId("asset:USD"))
    val btc            = TestAsset.runtime(AtomId("asset:BTC"))
    val xbt            = TestAsset.runtime(AtomId("asset:XBT"))
    val cents          = UniformGrid.create(usd.dimension, PositiveRational.exact(1, 100).toOption.get)
    val alternateCents = UniformGrid.create(usd.dimension, PositiveRational.exact(1, 100).toOption.get)
    val threeCents     = UniformGrid.create(usd.dimension, PositiveRational.exact(3, 100).toOption.get)
    val satoshis       = UniformGrid.create(btc.dimension, PositiveRational.exact(1, 100_000_000).toOption.get)
    val xbtSatoshis    = UniformGrid.create(xbt.dimension, PositiveRational.exact(1, 100_000_000).toOption.get)

  end Fixture

  test("runtime dimensions and anonymous uniform grids retain exact mathematical values"):
    val fixture  = new Fixture
    val dollars  = fixture.cents.fromCoordinate(12_345)
    val bitcoin  = fixture.satoshis.fromCoordinate(10_000_000)
    val sixCents = fixture.threeCents.fromCoordinate(2)
    val oneCent  = Quantity(fixture.usd.dimension, Rational(1, 100))

    assertEquals(fixture.cents.asQuantity(dollars).coefficient, Rational(2469, 20))
    assertEquals(fixture.satoshis.asQuantity(bitcoin).coefficient, Rational(1, 10))
    assertEquals(fixture.threeCents.asQuantity(sixCents).coefficient, Rational(3, 50))
    assert(oneCent.narrowExactlyTo(fixture.threeCents).isLeft)

  test("0.1 BTC at 60000.01 USD/BTC produces exact 6000.001 USD before projection"):
    val fixture   = new Fixture
    val amount    = fixture.satoshis.fromCoordinate(10_000_000)
    val usdPerBtc =
      Rate(
        fixture.btc.dimension,
        fixture.usd.dimension,
        Rational(6_000_001, 100)
      )
    val notional: Quantity[fixture.usd.D] = amount.applyRate(usdPerBtc, fixture.satoshis)

    assertEquals(notional.coefficient, Rational(6_000_001, 1000))
    assert(notional.narrowExactlyTo(fixture.cents).isLeft)

    val quantized = notional.quantizeTo(fixture.cents, QuantizationPolicy.HalfEven)
    val projected = fixture.cents.asQuantity(quantized.value)

    assertEquals(fixture.cents.coordinate(quantized.value), BigInt(600_000))
    assertEquals(quantized.residual.coefficient, Rational(1, 1000))
    assertEquals(projected.coefficient + quantized.residual.coefficient, notional.coefficient)

    val allocation =
      fixture.cents
        .fromCoordinate(1000)
        .allocateEvenly(PositiveInt(3).toOption.get, RemainderOrder.FirstToLast, fixture.cents)
    assertEquals(allocation.parts.map(fixture.cents.coordinate), Vector(BigInt(334), BigInt(333), BigInt(333)))

  test("an inverse calculation may produce a valid exact XBT quantity that is off the satoshi grid"):
    val fixture                          = new Fixture
    val inverseCoefficient               = Rational.one./(Rational(100_001, 2)).toOption.get
    val inverse: Quantity[fixture.xbt.D] =
      Quantity(fixture.xbt.dimension, inverseCoefficient)

    assertEquals(inverse.coefficient, Rational(2, 100_001))
    assert(inverse.narrowExactlyTo(fixture.xbtSatoshis).isLeft)

    val projected = inverse.quantizeTo(fixture.xbtSatoshis, QuantizationPolicy.HalfEven)
    val embedded  = projected.value.asQuantity(fixture.xbtSatoshis)
    assertEquals(embedded.coefficient + projected.residual.coefficient, inverse.coefficient)

  test("separately generated equal-quantum grids remain mathematically distinct"):
    val fixture    = new Fixture
    val coordinate = BigInt(2).pow(160) + 7
    assertEquals(fixture.satoshis.coordinate(fixture.satoshis.fromCoordinate(coordinate)), coordinate)

    val primary   = fixture.cents.fromCoordinate(100)
    val alternate = fixture.alternateCents.fromCoordinate(100)
    assert(primary.exactlyEquals(alternate, fixture.cents, fixture.alternateCents))
    assertEquals(SameGrid.between(fixture.cents, fixture.alternateCents), Left(AnonymousGridMismatch))

end FoundationExamplesSuite
