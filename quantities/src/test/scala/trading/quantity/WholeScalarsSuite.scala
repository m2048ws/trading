package trading.quantity

import munit.FunSuite

import trading.quantity.refinement.*
import trading.quantity.testkit.CompileAssertions.*

class WholeScalarsSuite extends FunSuite:
  test("positive whole validation accepts only positive arbitrary-precision integers"):
    val huge = BigInt(2).pow(256)
    assertEquals(PositiveWhole(huge).map(_.unrefined), Right(huge))
    assertEquals(PositiveWhole(0), Left(ExpectedPositive))
    assertEquals(PositiveWhole(-1), Left(ExpectedPositive))

  test("nonzero whole validation accepts both signs and rejects zero"):
    assertEquals(NonZeroWhole(7).map(_.unrefined), Right(BigInt(7)))
    assertEquals(NonZeroWhole(-7).map(_.unrefined), Right(BigInt(-7)))
    assertEquals(NonZeroWhole(0), Left(ExpectedNonZero))

  test("materialized allocation counts are positive Int values"):
    assertEquals(PositiveInt(3).map(_.unrefined), Right(3))
    assertEquals(PositiveInt(0), Left(ExpectedPositive))
    assertEquals(PositiveInt(-1), Left(ExpectedPositive))

  test("positive scalar implications and Int-to-BigInt widening do not revalidate"):
    val positive               = PositiveWhole(3).toOption.get
    val nonzero: NonZeroWhole  = positive.asNonZero
    val count                  = PositiveInt(3).toOption.get
    val widened: PositiveWhole = count.toPositiveWhole

    assertEquals(nonzero.unrefined, BigInt(3))
    assertEquals(widened.unrefined, BigInt(3))

  test("whole-scalar constructors do not accept floating-point values"):
    assertDoesNotCompile(
      """import trading.quantity.refinement.*; val invalid = PositiveWhole(1.5)"""
    )
    assertDoesNotCompile(
      """import trading.quantity.refinement.*; val invalid = NonZeroWhole(1.5f)"""
    )

end WholeScalarsSuite
