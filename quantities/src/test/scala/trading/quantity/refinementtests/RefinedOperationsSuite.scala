package trading.quantity.refinementtests

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.testkit.CompileAssertions.*

class RefinedOperationsSuite extends FunSuite:
  private val usd   = trading.quantity.testkit.TestAsset.runtime(AssetId("USD-refined-operations-suite"))
  private val cents =
    UniformGrid.create[usd.D](
      GridId("usd-cent-refined-operations-suite"),
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  test("nonnegative addition preserves refinement while subtraction weakens or checks"):
    val one: NonNegative[GridQuantity[usd.D, cents.G]] = NonNegative(cents.fromCoordinate(1)).toOption.get
    val two: NonNegative[GridQuantity[usd.D, cents.G]] = NonNegative(cents.fromCoordinate(2)).toOption.get
    val sum: NonNegative[GridQuantity[usd.D, cents.G]] = one.add(two)
    val difference: GridQuantity[usd.D, cents.G]       = one.subtract(two)

    assertEquals(cents.coordinate(sum.unrefined), BigInt(3))
    assertEquals(cents.coordinate(difference), BigInt(-1))
    assertEquals(one.subtractChecked(two), Left(ExpectedNonNegative))
    assertEquals(two.subtractChecked(one).map(value => cents.coordinate(value.unrefined)), Right(BigInt(1)))

  test("positive addition and exact division stay positive"):
    val left: Positive[Quantity[usd.D]] =
      Positive(Quantity(usd.dimension, 10)).toOption.get
    val right: Positive[Quantity[usd.D]] =
      Positive(Quantity(usd.dimension, 2)).toOption.get
    val divisor                           = PositiveWhole(3).toOption.get
    val sum: Positive[Quantity[usd.D]]    = left.add(right)
    val result: Positive[Quantity[usd.D]] = left.exactDivideBy(divisor)

    assertEquals(sum.unrefined.coefficient, Rational(12))
    assertEquals(result.unrefined.coefficient, Rational(10, 3))

  test("exact and grid division preserve each refinement law"):
    val nonnegativeExact = NonNegative(Quantity(usd.dimension, 0)).toOption.get
    val nonnegativeGrid  = NonNegative(cents.fromCoordinate(6)).toOption.get
    val positiveGrid     = Positive(cents.fromCoordinate(6)).toOption.get
    val nonzeroGrid      = NonZero(cents.fromCoordinate(-6)).toOption.get
    val positiveWhole    = PositiveWhole(2).toOption.get
    val nonzeroWhole     = NonZeroWhole(-2).toOption.get

    val first: NonNegative[Quantity[usd.D]]  = nonnegativeExact.exactDivideBy(positiveWhole)
    val second: NonNegative[Quantity[usd.D]] = nonnegativeGrid.exactDivideBy(positiveWhole, cents)
    val third: Positive[Quantity[usd.D]]     = positiveGrid.exactDivideBy(positiveWhole, cents)
    val fourth: NonZero[Quantity[usd.D]]     = nonzeroGrid.exactDivideBy(nonzeroWhole, cents)

    assertEquals(first.unrefined.coefficient, Rational.zero)
    assertEquals(second.unrefined.coefficient, Rational(3, 100))
    assertEquals(third.unrefined.coefficient, Rational(3, 100))
    assertEquals(fourth.unrefined.coefficient, Rational(3, 100))

  test("nonnegative and positive grid quotient/remainder results are nonnegative"):
    val nonnegative = NonNegative(cents.fromCoordinate(7)).toOption.get
    val positive    = Positive(cents.fromCoordinate(1)).toOption.get
    val divisor     = PositiveWhole(2).toOption.get
    val first       = nonnegative.quotRemBy(divisor, cents)
    val second      = positive.quotRemBy(divisor, cents)

    val firstQuotient: NonNegative[GridQuantity[usd.D, cents.G]]   = first.quotient
    val firstRemainder: NonNegative[GridQuantity[usd.D, cents.G]]  = first.remainder
    val secondQuotient: NonNegative[GridQuantity[usd.D, cents.G]]  = second.quotient
    val secondRemainder: NonNegative[GridQuantity[usd.D, cents.G]] = second.remainder

    assertEquals(cents.coordinate(firstQuotient.unrefined), BigInt(3))
    assertEquals(cents.coordinate(firstRemainder.unrefined), BigInt(1))
    assertEquals(cents.coordinate(secondQuotient.unrefined), BigInt(0))
    assertEquals(cents.coordinate(secondRemainder.unrefined), BigInt(1))

  test("positive quantization weakens to nonnegative because projection may be zero"):
    val source = Positive(Quantity(usd.dimension, Rational(1, 1000))).toOption.get
    val result: RefinedQuantization[NonNegative[GridQuantity[usd.D, cents.G]], usd.D] =
      source.quantizeTo(cents, QuantizationPolicy.HalfEven)

    assertEquals(cents.coordinate(result.value.unrefined), BigInt(0))
    assertEquals(result.residual.coefficient, Rational(1, 1000))

    val nonnegative = NonNegative(Quantity.zero[usd.D]).toOption.get
    val nonnegativeResult: RefinedQuantization[NonNegative[GridQuantity[usd.D, cents.G]], usd.D] =
      nonnegative.quantizeTo(cents, QuantizationPolicy.Floor)
    val positiveGrid = Positive(cents.fromCoordinate(1)).toOption.get
    val positiveGridResult: RefinedQuantization[NonNegative[GridQuantity[usd.D, cents.G]], usd.D] =
      positiveGrid.quantizeTo(cents, cents, QuantizationPolicy.HalfEven)

    assertEquals(cents.coordinate(nonnegativeResult.value.unrefined), BigInt(0))
    assertEquals(cents.coordinate(positiveGridResult.value.unrefined), BigInt(1))

  test("nonzero addition and quantization weaken when zero is possible"):
    val positive                                = NonZero(cents.fromCoordinate(1)).toOption.get
    val negative                                = NonZero(cents.fromCoordinate(-1)).toOption.get
    val sum: GridQuantity[usd.D, cents.G]       = positive.add(negative)
    val tiny                                    = NonZero(Quantity(usd.dimension, Rational(1, 1000))).toOption.get
    val projected: Quantization[usd.D, cents.G] = tiny.quantizeTo(cents, QuantizationPolicy.HalfEven)

    assertEquals(cents.coordinate(sum), BigInt(0))
    assertEquals(cents.coordinate(projected.value), BigInt(0))

  test("nonzero exact division stays nonzero while grid quotient may become zero"):
    val exact                             = NonZero(Quantity(usd.dimension, -7)).toOption.get
    val whole                             = NonZeroWhole(-2).toOption.get
    val divided: NonZero[Quantity[usd.D]] = exact.exactDivideBy(whole)
    assertEquals(divided.unrefined.coefficient, Rational(7, 2))

    val grid                                   = NonZero(cents.fromCoordinate(1)).toOption.get
    val result                                 = grid.quotRemBy(PositiveWhole(2).toOption.get, cents)
    val quotient: GridQuantity[usd.D, cents.G] = result.quotient
    assertEquals(cents.coordinate(quotient), BigInt(0))
    assertEquals(cents.coordinate(result.remainder.unrefined), BigInt(1))

  test("refinement loss is visible in public result types"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val usd = trading.quantity.testkit.TestAsset.runtime(AssetId("positive-quotient-type-suite"))
      val cents = UniformGrid.create[usd.D](GridId("positive-quotient-grid-suite"), GridVersion(1), usd.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val positive = Positive(cents.fromCoordinate(1)).toOption.get
      val invalid: Positive[GridQuantity[usd.D, cents.G]] =
        positive.quotRemBy(PositiveWhole(2).toOption.get, cents).quotient
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val usd = trading.quantity.testkit.TestAsset.runtime(AssetId("nonzero-quotient-type-suite"))
      val cents = UniformGrid.create[usd.D](GridId("nonzero-quotient-grid-suite"), GridVersion(1), usd.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val nonzero = NonZero(cents.fromCoordinate(1)).toOption.get
      val invalid: NonZero[GridQuantity[usd.D, cents.G]] =
        nonzero.quotRemBy(PositiveWhole(2).toOption.get, cents).quotient
      """

end RefinedOperationsSuite
