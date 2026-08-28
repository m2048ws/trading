package trading.quantity.refinement

import scala.compiletime.testing.typeCheckErrors

import munit.FunSuite

class RefinementPackageSpoofBoundarySuite extends FunSuite:

  inline def assertRejected(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.isEmpty then
      fail("expected downstream refinement-package source to be rejected")

  inline def assertAccepted(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.nonEmpty then
      fail(s"expected downstream prelude to compile: ${errors.map(_.message).mkString("; ")}")

  test("supported refinement-package prelude compiles"):
    assertAccepted:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("refinement-package-prelude"))
      val grid = UniformGrid.create(dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val exact = Quantity(dimension.dimension, 1)
      val coordinate = grid.fromCoordinate(1)
      val nonnegative = NonNegative(exact).toOption.get
      val nonzero = NonZero(exact).toOption.get
      val positive = Positive(coordinate).toOption.get
      val weakenedNonnegative = positive.asNonNegative
      val weakenedNonzero = positive.asNonZero
      """

  test("same-package source cannot manufacture refinements"):
    assertRejected:
      """
      import trading.quantity.refinement.*
      val forged: NonNegative[Int] = -1
      """
    assertRejected:
      """
      import trading.quantity.refinement.*
      val forged: Positive[BigInt] = BigInt(-1)
      """
    assertRejected:
      """
      import trading.quantity.refinement.*
      val forged: NonZero[BigInt] = BigInt(0)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val raw = Quantity(DimRef.one, -1)
      val forged: NonNegative[Quantity[One]] = raw
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val raw = Quantity.zero[One]
      val forged: Positive[Quantity[One]] = raw
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val raw = Quantity.zero[One]
      val forged: NonZero[Quantity[One]] = raw
      """

  test("same-package source cannot invoke weakening with an unrefined value"):
    assertRejected:
      """
      import trading.quantity.refinement.*
      val forged: NonNegative[BigInt] = Positive.asNonNegative(BigInt(0))
      """
    assertRejected:
      """
      import trading.quantity.refinement.*
      val forged: NonZero[BigInt] = Positive.asNonZero(BigInt(0))
      """

  test("same-package source cannot construct or supply Sign"):
    assertRejected:
      """
      import trading.quantity.refinement.*
      val forged = new Sign[Int](_ => 1)
      """
    assertRejected:
      """
      import trading.quantity.refinement.*
      given Sign[String] = new Sign[String](_ => 1)
      val forged = Positive("")
      """

end RefinementPackageSpoofBoundarySuite
