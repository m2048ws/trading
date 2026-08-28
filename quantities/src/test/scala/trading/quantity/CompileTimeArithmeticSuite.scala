package trading.quantity

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class CompileTimeArithmeticSuite extends FunSuite:
  test("unlike-dimension addition does not compile"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val usd = trading.quantity.testkit.TestAsset.runtime(AtomId("USD-compile-suite"))
      val btc = trading.quantity.testkit.TestAsset.runtime(AtomId("BTC-compile-suite"))
      val cents = UniformGrid.create[usd.D](usd.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      val satoshis = UniformGrid.create[btc.D](btc.dimension,
        PositiveRational.exact(1, 100000000).toOption.get
      )

      val invalid = cents.fromCoordinate(1) + satoshis.fromCoordinate(1)
    """

  test("expected types cannot select an implicit grid projection"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val usd = trading.quantity.testkit.TestAsset.runtime(AtomId("USD-expected-type-suite"))
      val cents = UniformGrid.create[usd.D](usd.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      val threeCents = UniformGrid.create[usd.D](usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )

      val projected: GridQuantity[usd.D, cents.G] =
        cents.fromCoordinate(1).addExact(threeCents.fromCoordinate(1), cents, threeCents)
    """

  test("rounding-policy and algebra-instance imports do not alter exact arithmetic") {
    assertCompiles(
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      object RoundingImports:
        trait Policy
        given Policy = new Policy {}

      object AlgebraImports:
        trait AdditiveMarker[A]
        given [D <: Dim, G]: AdditiveMarker[GridQuantity[D, G]] =
          new AdditiveMarker[GridQuantity[D, G]] {}

      import RoundingImports.given
      import AlgebraImports.given

      val usd = trading.quantity.testkit.TestAsset.runtime(AtomId("USD-import-coherence-suite"))
      val cents = UniformGrid.create[usd.D](usd.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      val threeCents = UniformGrid.create[usd.D](usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )

      val exact: Quantity[usd.D] =
        cents.fromCoordinate(1).addExact(threeCents.fromCoordinate(1), cents, threeCents)
      val meaning: Rational = exact.coefficient
    """
    )
  }

  test("a rate cannot be applied to a quantity with the wrong source dimension"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val usd = trading.quantity.testkit.TestAsset.runtime(AtomId("USD-rate-mismatch-suite"))
      val btc = trading.quantity.testkit.TestAsset.runtime(AtomId("BTC-rate-mismatch-suite"))
      val eur = trading.quantity.testkit.TestAsset.runtime(AtomId("EUR-rate-mismatch-suite"))
      val bitcoin = Quantity(btc.dimension, 1)
      val usdToEur = Rate(usd.dimension, eur.dimension, Rational(9, 10))

      val invalid = bitcoin.applyRate(usdToEur)
    """

end CompileTimeArithmeticSuite
