package trading.quantity.testkit

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*

class TestSupportSuite extends ScalaCheckSuite:
  import CompileAssertions.*
  import ExactGenerators.*

  private object UsdTag
  private object BtcTag
  private type Usd = Atom[UsdTag.type]
  private type Btc = Atom[BtcTag.type]

  property("exact rational generators always produce canonical values"):
    forAll(rational): value =>
      assert:
        value.denominator > 0
      assertEquals(
        value.numerator
          .gcd:
            value.denominator
        ,
        BigInt(1)
      )

  property("positive rational generators always produce positive values"):
    forAll(positiveRational): value =>
      assert:
        value.unrefined.signum > 0

  test("compile helpers assert dimension result types"):
    assertSameType[Divide[Usd, Btc], Times[Usd, Inverse[Btc]]]
    assertCompiles:
      """
      import trading.quantity.*
      type Asset = Atom["test-support:asset"]
      val key: DimKey = DimKey.atom(AtomId("asset"))
    """

  test("compile helpers detect hidden opaque constructors"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      type Asset = Atom["test-support:asset"]
      val value: Quantity[Asset] = Rational.one
    """

  test("compile helpers detect forbidden floating-point construction"):
    assertDoesNotCompile:
      """
      import trading.quantity.refinement.*
      val quantum = PositiveRational(0.01)
    """

end TestSupportSuite
