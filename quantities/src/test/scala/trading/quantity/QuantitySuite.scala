package trading.quantity

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*

class QuantitySuite extends ScalaCheckSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "USD-quantity-suite"
  private val cents =
    UniformGrid.create[usd.D](usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  test("Quantity exposes the project-owned trading.quantity.Rational"):
    val coefficient: trading.quantity.Rational = Quantity(usd.dimension, 10).coefficient

    assertEquals(
      coefficient,
      trading.quantity
        .Rational:
          10
    )

  property("grid embedding into Quantity is exact"):
    forAll(coordinate): coordinateValue =>
      val gridValue =
        cents.fromCoordinate:
          coordinateValue
      assertEquals(
        gridValue
          .asQuantity:
            cents
          .coefficient,
        Rational(coordinateValue, 100)
      )

  property("same-dimension quantity addition and subtraction are exact"):
    forAll(rational, rational): (leftCoefficient, rightCoefficient) =>
      val left  = Quantity(usd.dimension, leftCoefficient)
      val right = Quantity(usd.dimension, rightCoefficient)

      assertEquals((left + right).coefficient, leftCoefficient + rightCoefficient)
      assertEquals((left - right).coefficient, leftCoefficient - rightCoefficient)

  test("integer, text, and finite-decimal constructors preserve exact values"):
    assertEquals(
      Quantity(usd.dimension, 42).coefficient,
      Rational:
        42
    )
    assertEquals(
      Quantity(usd.dimension, "6000.001").toOption.get.coefficient,
      Rational(6000001, 1000)
    )
    assertEquals(
      Quantity(
        usd.dimension,
        BigDecimal:
          "0.00000001"
        .bigDecimal
      ).toOption.get.coefficient,
      Rational(1, 100000000)
    )

  test("primitive integer apply overloads widen exactly through BigInt"):
    val intCoefficient  = Int.MinValue
    val longCoefficient = Long.MaxValue

    assertEquals(
      Quantity(usd.dimension, intCoefficient).coefficient,
      Quantity(usd.dimension, BigInt(intCoefficient)).coefficient
    )
    assertEquals(
      Quantity(usd.dimension, longCoefficient).coefficient,
      Quantity(usd.dimension, BigInt(longCoefficient)).coefficient
    )

end QuantitySuite
