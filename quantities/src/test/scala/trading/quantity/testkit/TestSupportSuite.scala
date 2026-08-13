package trading.quantity.testkit

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*

class TestSupportSuite extends ScalaCheckSuite:
  import CompileAssertions.*
  import ExactGenerators.*

  private sealed trait UsdTag
  private sealed trait BtcTag
  private type Usd = Atom[UsdTag]
  private type Btc = Atom[BtcTag]

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
      sealed trait AssetTag
      type Asset = Atom[AssetTag]
      val key: DimensionKey = DimensionKey.atom(AtomId("asset"))
    """

  test("compile helpers detect hidden opaque constructors"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      sealed trait AssetTag
      type Asset = Atom[AssetTag]
      val value: Quantity[Asset] = Rational.one
    """

  test("compile helpers detect forbidden floating-point construction"):
    assertDoesNotCompile:
      """
      import trading.quantity.refinement.*
      val quantum = PositiveRational(0.01)
    """

end TestSupportSuite
