package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*

class CrossGridArithmeticSuite extends ScalaCheckSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-cross-grid-suite"
  private val cents =
    UniformGrid.create[usd.D](
      GridId:
        "usd-cent-cross-grid-suite"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )
  private val threeCents =
    UniformGrid.create[usd.D](
      GridId:
        "usd-three-cent-cross-grid-suite"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(3, 100).toOption.get
    )

  property("cross-grid addition and subtraction return exact quantities"):
    forAll(coordinate, coordinate): (centCoordinate, threeCentCoordinate) =>
      val centValue =
        cents.fromCoordinate:
          centCoordinate
      val threeCentValue =
        threeCents.fromCoordinate:
          threeCentCoordinate
      val left  = Rational(centCoordinate, 100)
      val right = Rational(threeCentCoordinate * 3, 100)

      assertEquals(centValue.addExact(threeCentValue, cents, threeCents).coefficient, left + right)
      assertEquals(centValue.subtractExact(threeCentValue, cents, threeCents).coefficient, left - right)

  test("cross-grid equality compares mathematical values, not coordinates or identities"):
    val threeCentValue = threeCents.fromCoordinate(1)
    assert:
      cents.fromCoordinate(3).exactlyEquals(threeCentValue, cents, threeCents)
    assert:
      !cents.fromCoordinate(1).exactlyEquals(threeCentValue, cents, threeCents)

  property("cross-grid comparison uses exact mathematical ordering"):
    forAll(coordinate, coordinate): (centCoordinate, threeCentCoordinate) =>
      val centValue =
        cents.fromCoordinate:
          centCoordinate
      val threeCentValue =
        threeCents.fromCoordinate:
          threeCentCoordinate
      val expected =
        Rational(centCoordinate, 100).compare:
          Rational(threeCentCoordinate * 3, 100)

      assertEquals(centValue.compareExact(threeCentValue, cents, threeCents), expected)

end CrossGridArithmeticSuite
