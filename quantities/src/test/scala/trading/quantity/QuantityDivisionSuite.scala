package trading.quantity

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*

class QuantityDivisionSuite extends ScalaCheckSuite:
  private val usd = trading.quantity.testkit.TestAsset.runtime(AssetId("USD-division-suite"))
  private val btc = trading.quantity.testkit.TestAsset.runtime(AssetId("BTC-division-suite"))
  private type UsdPerBtc = Divide[usd.D, btc.D]
  private val cents =
    UniformGrid.create[usd.D](
      GridId("usd-cent-division-suite"),
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  test("generic nonzero construction rejects zero exact and grid quantities"):
    val exactZero = Quantity.zero[usd.D](using usd.dimension)
    val gridZero  = cents.fromCoordinate(0)

    assertEquals(NonZero(exactZero), Left(ExpectedNonZero))
    assertEquals(NonZero(gridZero), Left(ExpectedNonZero))
    assertEquals(NonZero(cents.asQuantity(gridZero)), Left(ExpectedNonZero))

  test("equal-dimension division returns an exact dimensionless ratio"):
    val tenUsd                            = Quantity(usd.dimension, 10)
    val threeUsd                          = Quantity(usd.dimension, 3)
    val divisor: NonZero[Quantity[usd.D]] = NonZero(threeUsd).toOption.get
    val result: Ratio                     = tenUsd.ratioTo(divisor)

    assertEquals(result.coefficient, Rational(10, 3))

  test("generic checked division preserves an equal-dimension Divide expression"):
    val tenUsd                                 = Quantity(usd.dimension, 10)
    val threeUsd                               = Quantity(usd.dimension, 3)
    val divisor: NonZero[Quantity[usd.D]]      = NonZero(threeUsd).toOption.get
    val result: Quantity[Divide[usd.D, usd.D]] = tenUsd.divideBy(divisor)

    assertEquals(result.coefficient, Rational(10, 3))

  test("unlike-dimension division returns the exact quotient dimension"):
    val tenUsd                            = Quantity(usd.dimension, 10)
    val twoBtc                            = Quantity(btc.dimension, 2)
    val divisor: NonZero[Quantity[btc.D]] = NonZero(twoBtc).toOption.get
    val result: Quantity[UsdPerBtc]       = tenUsd.divideBy(divisor)

    assertEquals(result.coefficient, Rational(5))

  test("grid divisors embed exactly before generic nonzero refinement"):
    val tenUsd                            = cents.fromCoordinate(1000)
    val threeUsd                          = cents.fromCoordinate(300)
    val exactDivisor                      = cents.asQuantity(threeUsd)
    val divisor: NonZero[Quantity[usd.D]] = NonZero(exactDivisor).toOption.get
    val result: Ratio                     = tenUsd.ratioTo(divisor, cents)

    assertEquals(exactDivisor.coefficient, Rational(3))
    assertEquals(result.coefficient, Rational(10, 3))

  test("grid quantity division uses the raw exact quotient"):
    val tenUsd                            = cents.fromCoordinate(1000)
    val twoBtc                            = Quantity(btc.dimension, 2)
    val divisor: NonZero[Quantity[btc.D]] = NonZero(twoBtc).toOption.get
    val result: Quantity[UsdPerBtc]       = tenUsd.divideBy(divisor, cents)
    val embedded: Quantity[UsdPerBtc]     = cents.asQuantity(tenUsd).divideBy(divisor)

    assertEquals(result.coefficient, Rational(5))
    assertEquals(result.coefficient, embedded.coefficient)

  property("quantity-by-quantity division preserves the exact quotient law"):
    forAll(rational, nonZeroRational): (dividendCoefficient, divisorCoefficient) =>
      val dividend                      = Quantity(usd.dimension, dividendCoefficient)
      val divisorQuantity               = Quantity(btc.dimension, divisorCoefficient)
      val divisor                       = NonZero(divisorQuantity).toOption.get
      val quotient: Quantity[UsdPerBtc] = dividend.divideBy(divisor)

      assertEquals(quotient.coefficient * divisorCoefficient, dividendCoefficient)
      assertEquals(quotient.coefficient, dividendCoefficient./(divisorCoefficient).toOption.get)

  property("grid quantity division remains exact for arbitrary coordinates"):
    forAll(coordinate, nonZeroBigInt): (dividendCoordinate, divisorCoordinate) =>
      val dividend        = cents.fromCoordinate(dividendCoordinate)
      val divisorQuantity = cents.fromCoordinate(divisorCoordinate)
      val divisor         = NonZero(cents.asQuantity(divisorQuantity)).toOption.get
      val quotient: Ratio = dividend.ratioTo(divisor, cents)

      assertEquals(quotient.coefficient, Rational(dividendCoordinate, divisorCoordinate))

end QuantityDivisionSuite
