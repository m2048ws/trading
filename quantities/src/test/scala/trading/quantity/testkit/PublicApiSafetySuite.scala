package trading.quantity.testkit

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class PublicApiSafetySuite extends FunSuite:
  test("public exact constructors reject Float and Double inputs"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val asset = trading.quantity.testkit.TestAsset.runtime(AssetId("floating-apply-double"))
      val value = Quantity(asset.dimension, 0.1d)
    """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val asset = trading.quantity.testkit.TestAsset.runtime(AssetId("floating-apply-float"))
      val value = Quantity(asset.dimension, 0.1f)
    """
    assertDoesNotCompile:
      """
      import trading.quantity.refinement.*
      val quantum = PositiveRational(0.03f)
    """

  test("named Quantity constructors are unavailable"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val value = Quantity.fromRational(DimRef.one, Rational.one)
    """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val value = Quantity.fromInteger(DimRef.one, BigInt(1))
    """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val value = Quantity.fromDecimal(DimRef.one, "0.03")
    """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val value = Quantity.fromFiniteDecimal(DimRef.one, BigDecimal("0.03").bigDecimal)
    """

  test("quantization cannot be invoked without an explicit policy"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      import trading.quantity.refinement.*
      val asset = trading.quantity.testkit.TestAsset.runtime(AssetId("parameterless-quantization"))
      val grid = UniformGrid.create[asset.D](
        GridId("parameterless-grid"), GridVersion(1), asset.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      val source = Quantity(asset.dimension, Rational(1, 3))
      val rounded = source.quantizeTo(grid)
    """

  test("raw opaque representations have no public construction path"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      sealed trait DTag
      type D = Atom[DTag]
      val forged: Quantity[D] = Rational.one
    """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      sealed trait DTag
      sealed trait GTag
      type D = Atom[DTag]
      type G = GTag
      val forged: GridQuantity[D, G] = BigInt(1)
    """

  test("expected result types cannot select a rounded arithmetic meaning"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val asset = trading.quantity.testkit.TestAsset.runtime(AssetId("expected-type-audit"))
      val cents = UniformGrid.create[asset.D](
        GridId("expected-cent"), GridVersion(1), asset.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      val threeCents = UniformGrid.create[asset.D](
        GridId("expected-three-cent"), GridVersion(1), asset.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )
      val selected: GridQuantity[asset.D, cents.G] =
        cents.fromCoordinate(1).addExact(
          threeCents.fromCoordinate(1), cents, threeCents
        )
    """

  test("exact non-floating construction remains available"):
    assertCompiles:
      """
      import trading.quantity.*
      val asset = trading.quantity.testkit.TestAsset.runtime(AssetId("exact-construction-control"))
      val fromIntegerLiteral: Quantity[asset.D] = Quantity(asset.dimension, 42)
      val fromLongLiteral: Quantity[asset.D] = Quantity(asset.dimension, 42L)
      val integerValue: Int = Int.MinValue
      val longValue: Long = Long.MaxValue
      val fromIntegerValue: Quantity[asset.D] = Quantity(asset.dimension, integerValue)
      val fromLongValue: Quantity[asset.D] = Quantity(asset.dimension, longValue)
      val fromApplyText = Quantity(asset.dimension, "0.03")
      val fromApplyFinite = Quantity(
        asset.dimension,
        BigDecimal("0.03").bigDecimal
      )
      val fromApplyRatio = Quantity(asset.dimension, Rational(3, 100))
    """

end PublicApiSafetySuite
