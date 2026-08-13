package trading.quantity

import munit.FunSuite

import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.testkit.CompileAssertions.*

class ExactScalarDivisionSuite extends FunSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-exact-scalar-division-suite"
  private val cents =
    UniformGrid.create[usd.D](
      GridId:
        "usd-cent-exact-scalar-division-suite"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  test("grid exactDivideBy returns a rational without projection"):
    val tenUsd =
      cents.fromCoordinate:
        1000
    val three                   = NonZeroWhole(3).toOption.get
    val result: Quantity[usd.D] = tenUsd.exactDivideBy(three, cents)

    assertEquals(result.coefficient, Rational(10, 3))

  test("rational exactDivideBy preserves exact signs and fractions"):
    val value         = Quantity(usd.dimension, Rational(5, 7))
    val negativeThree =
      NonZeroWhole(-3).toOption.get
    val result: Quantity[usd.D] =
      value.exactDivideBy:
        negativeThree

    assertEquals(result.coefficient, Rational(-5, 21))

  test("10.00 USD divided by three has three explicit scalar meanings"):
    val tenUsd =
      cents.fromCoordinate:
        1000
    val nonzeroThree    = NonZeroWhole(3).toOption.get
    val positiveThree   = PositiveWhole(3).toOption.get
    val threeRecipients = PositiveInt(3).toOption.get

    val exact: Quantity[usd.D]                               = tenUsd.exactDivideBy(nonzeroThree, cents)
    val quotRem: QuotRem[GridQuantity[usd.D, cents.G]]       = tenUsd.quotRemBy(positiveThree, cents)
    val allocation: Allocation[GridQuantity[usd.D, cents.G]] =
      tenUsd.allocateEvenly(threeRecipients, RemainderOrder.FirstToLast, cents)

    assertEquals(exact.coefficient, Rational(10, 3))
    assertEquals(
      cents.coordinate(quotRem.quotient),
      BigInt:
        333
    )
    assertEquals(cents.coordinate(quotRem.remainder), BigInt(1))
    assertEquals(
      allocation.parts
        .map:
          cents.coordinate
      ,
      Vector(
        BigInt:
          334
        ,
        BigInt:
          333
        ,
        BigInt:
          333
      )
    )

  test("expected types and unrelated imports cannot change exact scalar division") {
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val usd = trading.quantity.testkit.TestAsset.runtime(AssetId("scalar-expected-type-suite"))
      val cents = UniformGrid.create[usd.D](
        GridId("scalar-expected-grid-suite"), GridVersion(1), usd.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      val divisor = NonZeroWhole(3).toOption.get
      val projected: GridQuantity[usd.D, cents.G] =
        cents.fromCoordinate(1000).exactDivideBy(divisor, cents)
    """

    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      object UnrelatedImports:
        trait RoundingContext
        trait AlgebraContext[A]
        given RoundingContext = new RoundingContext {}
        given [A]: AlgebraContext[A] = new AlgebraContext[A] {}

      import UnrelatedImports.given

      val usd = trading.quantity.testkit.TestAsset.runtime(AssetId("scalar-import-suite"))
      val cents = UniformGrid.create[usd.D](
        GridId("scalar-import-grid-suite"), GridVersion(1), usd.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      val divisor = NonZeroWhole(3).toOption.get
      val exact: Quantity[usd.D] =
        cents.fromCoordinate(1000).exactDivideBy(divisor, cents)
    """
  }

end ExactScalarDivisionSuite
