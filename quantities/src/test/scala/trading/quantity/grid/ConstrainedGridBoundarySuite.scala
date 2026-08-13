package trading.quantity.grid

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.quantizeTo
import trading.quantity.refinement.*

class ConstrainedGridBoundarySuite extends FunSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-constrained-boundary-suite"
  private val cents =
    UniformGrid.create[usd.D](
      GridId:
        "usd-cent-constrained-boundary-suite"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  test("validation and constrained encoding reject off-grid values without rounding"):
    val source = Quantity(usd.dimension, Rational(11, 1000))

    assert:
      GridConstraint
        .validate(cents):
          source
        .isLeft
    assert:
      ConstrainedGridEncoding
        .encodeExact(cents):
          source
        .isLeft
    assertEquals(source.coefficient, Rational(11, 1000))

  test("quantization must be separately named before a projected value can be encoded"):
    val source         = Quantity(usd.dimension, Rational(11, 1000))
    val projected      = source.quantizeTo(cents, QuantizationPolicy.HalfEven).value
    val projectedExact =
      cents.asQuantity:
        projected

    assertEquals(
      ConstrainedGridEncoding.encodeExact(cents):
        projectedExact
      ,
      Right:
        GridCoordinateEncoding(cents.key, BigInt(1))
    )

  test("already representable exact values validate and encode unchanged"):
    val source = Quantity(usd.dimension, Rational(123, 100))

    assertEquals(
      GridConstraint
        .validate(cents):
          source
        .map:
          cents.coordinate
      ,
      Right:
        BigInt:
          123
    )
    assertEquals(
      ConstrainedGridEncoding.encodeExact(cents):
        source
      ,
      Right:
        GridCoordinateEncoding(
          cents.key,
          BigInt:
            123
        )
    )

    val encoding = ConstrainedGridEncoding.encodeExact(cents)(source).toOption.get
    assertEquals(encoding.localGridKey, cents.key)

end ConstrainedGridBoundarySuite
