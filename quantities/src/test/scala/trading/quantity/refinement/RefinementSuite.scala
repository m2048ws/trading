package trading.quantity.refinement

import munit.FunSuite

import trading.quantity.*
import trading.quantity.testkit.CompileAssertions.*

class RefinementSuite extends FunSuite:
  private val usd   = trading.quantity.testkit.TestAsset.runtime(AssetId("USD-refinement-suite"))
  private val cents =
    UniformGrid.create[usd.D](
      GridId("usd-cent-refinement-suite"),
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  test("generic constructors classify Int and BigInt exactly"):
    assertEquals(NonNegative(-1), Left(ExpectedNonNegative))
    assertEquals(NonNegative(0).map(_.unrefined), Right(0))
    assertEquals(NonZero(-1).map(_.unrefined), Right(-1))
    assertEquals(NonZero(0), Left(ExpectedNonZero))
    assertEquals(Positive(0), Left(ExpectedPositive))
    assertEquals(Positive(1).map(_.unrefined), Right(1))
    assertEquals(NonZero(BigInt(0)), Left(ExpectedNonZero))
    assertEquals(NonZero(BigInt(-1)).map(_.unrefined), Right(BigInt(-1)))
    val huge = BigInt(2).pow(1024)
    assertEquals(NonNegative(huge).map(_.unrefined), Right(huge))
    assertEquals(Positive(huge).map(_.unrefined), Right(huge))

  test("generic constructors classify exact Rational values"):
    assertEquals(NonNegative(Rational(-1, 3)), Left(ExpectedNonNegative))
    assertEquals(NonNegative(Rational.zero).map(_.unrefined), Right(Rational.zero))
    assertEquals(Positive(Rational(1, 3)).map(_.unrefined), Right(Rational(1, 3)))
    assertEquals(NonZero(Rational(-1, 3)).map(_.unrefined), Right(Rational(-1, 3)))
    assertEquals(NonZero(Rational.zero), Left(ExpectedNonZero))

  test("generic constructors classify exact and grid quantities"):
    val negativeExact = Quantity(usd.dimension, -1)
    val zeroExact     = Quantity.zero[usd.D]
    val positiveExact = Quantity(usd.dimension, Rational(1, 3))
    val negativeGrid  = cents.fromCoordinate(-1)
    val zeroGrid      = cents.fromCoordinate(0)
    val positiveGrid  = cents.fromCoordinate(BigInt(2).pow(512))

    assertEquals(NonNegative(negativeExact), Left(ExpectedNonNegative))
    assertEquals(NonNegative(zeroExact).map(_.unrefined.coefficient), Right(Rational.zero))
    assertEquals(Positive(positiveExact).map(_.unrefined.coefficient), Right(Rational(1, 3)))
    assertEquals(Positive(zeroExact), Left(ExpectedPositive))
    assertEquals(NonZero(zeroExact), Left(ExpectedNonZero))
    assertEquals(NonZero(negativeExact).map(_.unrefined.coefficient), Right(Rational(-1)))
    assertEquals(NonZero(negativeGrid).map(value => cents.coordinate(value.unrefined)), Right(BigInt(-1)))
    assertEquals(NonNegative(zeroGrid).map(value => cents.coordinate(value.unrefined)), Right(BigInt(0)))
    assertEquals(
      Positive(positiveGrid).map(value => cents.coordinate(value.unrefined)),
      Right(BigInt(2).pow(512))
    )
    assertEquals(Positive(zeroGrid), Left(ExpectedPositive))

  test("positive values weaken directly to nonnegative and nonzero values"):
    val positive: Positive[BigInt]       = Positive(BigInt(7)).toOption.get
    val nonnegative: NonNegative[BigInt] = positive.asNonNegative
    val nonzero: NonZero[BigInt]         = positive.asNonZero
    val raw: BigInt                      = positive.unrefined

    assertEquals(nonnegative.unrefined, BigInt(7))
    assertEquals(nonzero.unrefined, BigInt(7))
    assertEquals(raw, BigInt(7))

  test("scalar vocabulary is aliases over the generic refinements"):
    assertSameType[PositiveWhole, Positive[BigInt]]
    assertSameType[NonZeroWhole, NonZero[BigInt]]
    assertSameType[PositiveInt, Positive[Int]]
    assertSameType[PositiveRational, Positive[Rational]]

  test("raw carriers cannot be assigned to refinements"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val exact = Quantity(DimRef.one, 1)
      sealed trait G
      val grid: GridQuantity[One, G] = GridQuantity.zero
      val checkedExact = Positive(exact).toOption.get
      val checkedGrid = NonNegative(grid).toOption.get
      """
    assertDoesNotCompile("""import trading.quantity.refinement.*; val forged: Positive[Int] = 1""")
    assertDoesNotCompile("""import trading.quantity.refinement.*; val forged: NonZero[BigInt] = BigInt(1)""")
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged: NonNegative[Rational] = Rational.zero
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val raw = Quantity(DimRef.one, 1)
      val forged: Positive[Quantity[One]] = raw
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      sealed trait G
      val raw: GridQuantity[One, G] = GridQuantity.zero
      val forged: NonZero[GridQuantity[One, G]] = raw
      """

  test("the sign capability is closed to downstream implementations"):
    assertDoesNotCompile:
      """
      import trading.quantity.refinement.*
      val forged = new Sign[Int](value => -java.lang.Integer.signum(value))
      """
    assertDoesNotCompile:
      """
      import trading.quantity.refinement.*
      given Sign[String] = new Sign[String](_ => 1)
      """

end RefinementSuite
