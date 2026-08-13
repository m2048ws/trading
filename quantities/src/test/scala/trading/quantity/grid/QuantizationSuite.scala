package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.grid.quantizeTo
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*

class QuantizationSuite extends ScalaCheckSuite:
  import QuantizationPolicy.*

  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-quantization-suite"
  private val cents =
    UniformGrid.create[usd.D](
      GridId:
        "usd-cent-quantization-suite"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )
  private val threeCents =
    UniformGrid.create[usd.D](
      GridId("usd-three-cent-quantization-suite"),
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(3, 100).toOption.get
    )
  private val twoFifteenths =
    UniformGrid.create[usd.D](
      GridId("usd-two-fifteenths-quantization-suite"),
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(2, 15).toOption.get
    )
  private val policies =
    Vector(
      Floor,
      Ceiling,
      TowardZero,
      AwayFromZero,
      HalfEven,
      HalfOdd,
      HalfUp,
      HalfDown,
      HalfTowardZero,
      HalfAwayFromZero
    )

  property("every quantization exactly conserves source = projected + residual"):
    forAll(
      rational,
      Gen.oneOf:
        policies
    ): (coefficient, policy) =>
      val source    = Quantity(usd.dimension, coefficient)
      val result    = source.quantizeTo(cents, policy)
      val projected =
        cents
          .asQuantity:
            result.value
          .coefficient

      assertEquals(projected + result.residual.coefficient, coefficient)

  test("6000.001 USD quantizes to cents with auditable residuals"):
    val source  = Quantity(usd.dimension, "6000.001").toOption.get
    val nearest = source.quantizeTo(cents, HalfEven)
    val upward  = source.quantizeTo(cents, Ceiling)

    assertEquals(
      cents.coordinate:
        nearest.value
      ,
      BigInt:
        600000
    )
    assertEquals(nearest.residual.coefficient, Rational(1, 1000))
    assertEquals(
      cents.coordinate:
        upward.value
      ,
      BigInt:
        600001
    )
    assertEquals(upward.residual.coefficient, Rational(-9, 1000))

  property("floor, ceiling, and nearest residuals satisfy their quantum bounds"):
    forAll(rational): coefficient =>
      val source          = Quantity(usd.dimension, coefficient)
      val floorResidual   = source.quantizeTo(cents, Floor).residual.coefficient
      val ceilingResidual = source.quantizeTo(cents, Ceiling).residual.coefficient
      val nearestResidual = source.quantizeTo(cents, HalfEven).residual.coefficient
      val quantum         = cents.quantum.unrefined

      assert:
        floorResidual.signum >= 0 &&
        floorResidual.compare:
          quantum
        < 0
      assert:
        ceilingResidual.compare:
          -quantum
        > 0 && ceilingResidual.signum <= 0
      assert:
        nearestResidual.abs
          .compare:
            quantum * Rational(1, 2)
        <= 0

  test("already representable values have zero residual under every policy"):
    val source = Quantity(usd.dimension, Rational(123, 100))
    policies.foreach: policy =>
      val result = source.quantizeTo(cents, policy)
      assertEquals(
        cents.coordinate:
          result.value
        ,
        BigInt:
          123
      )
      assertEquals(result.residual.coefficient, Rational.zero)

  test("quantization onto 0.03 handles boundaries, large coordinates, and nontrivial source quanta exactly"):
    val boundary  = Quantity(usd.dimension, Rational(7, 100))
    val projected = boundary.quantizeTo(threeCents, HalfEven)
    assertEquals(threeCents.coordinate(projected.value), BigInt(2))
    assertEquals(projected.residual.coefficient, Rational(1, 100))

    val largeCoordinate = BigInt(10).pow(120) + 17
    val source          = twoFifteenths.fromCoordinate(largeCoordinate)
    policies.foreach: policy =>
      val result         = source.quantizeTo(twoFifteenths, threeCents, policy)
      val sourceExact    = twoFifteenths.asQuantity(source).coefficient
      val projectedExact = threeCents.asQuantity(result.value).coefficient
      assertEquals(projectedExact + result.residual.coefficient, sourceExact)

end QuantizationSuite
