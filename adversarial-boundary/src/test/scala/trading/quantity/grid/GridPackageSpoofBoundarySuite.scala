package trading.quantity.grid

import scala.compiletime.testing.typeCheckErrors

import munit.FunSuite

class GridPackageSpoofBoundarySuite extends FunSuite:

  inline def assertRejected(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.isEmpty then
      fail("expected downstream grid-package source to be rejected")

  inline def assertAccepted(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.nonEmpty then
      fail(s"expected downstream grid prelude to compile: ${errors.map(_.message).mkString("; ")}")

  test("supported grid-package evidence prelude compiles"):
    assertAccepted:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("grid-package-prelude"))
      val quantum = PositiveRational.exact(1, 100).toOption.get
      val left = UniformGrid.create(GridId("grid-package-prelude"), GridVersion(1), dimension.dimension, quantum)
      val right = UniformGrid.create(GridId("grid-package-prelude"), GridVersion(1), dimension.dimension, quantum)
      val sameGrid = SameGrid.between(left, right)
      val sameQuantum = SameQuantum.between(left, right)
      val embedding = Embedding.between(left, right)
      """

  test("grid-package extensions retain witness-controlled coordinate construction"):
    assertAccepted:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      import trading.quantity.refinement.*
      val quantum = PositiveRational.exact(1, 100).toOption.get
      val divisor = PositiveWhole(3).toOption.get
      val count = PositiveInt(3).toOption.get
      val grid = UniformGrid.create(GridId("grid-package-operations"), GridVersion(1), DimRef.one, quantum)
      val value = grid.fromCoordinate(10)
      val divided = value.quotRemBy(divisor, grid)
      val allocated = value.allocateEvenly(count, RemainderOrder.FirstToLast, grid)
      """

  test("same-package source cannot invoke grid-evidence constructors"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      val forged = new SameGrid[One, Unit, One, Unit]
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      val forged = new SameQuantum[One, Unit, One, Unit]
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      import trading.quantity.refinement.*
      val quantum = PositiveRational.exact(1, 100).toOption.get
      val left = UniformGrid.create(GridId("private-embedding-left"), GridVersion(1), DimRef.one, quantum)
      val right = UniformGrid.create(GridId("private-embedding-right"), GridVersion(1), DimRef.one, quantum)
      val forged = new Embedding[One, left.G, One, right.G](BigInt(1), left, right)
      """

  test("same-package source cannot add grid-error or quantization authority"):
    assertRejected:
      """
      import trading.quantity.grid.*
      case object ForgedGridError extends GridError
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      val forged = new QuantizationPolicy:
        def roundCoordinate(value: Rational): BigInt = BigInt(0)
        def acceptsResidual(value: Rational, coordinate: BigInt): Boolean = true
      """

  test("same-package source cannot construct checked grid results directly"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.grid.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("private-quantization-result"))
      val grid = UniformGrid.create(GridId("private-quantization-result"), GridVersion(1), dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val forged = new Quantization(grid.fromCoordinate(1), Quantity(dimension.dimension, 0))
      """
    assertRejected:
      """
      import trading.quantity.grid.*
      val forged = new Allocation(Vector.empty[Int])
      """

end GridPackageSpoofBoundarySuite
