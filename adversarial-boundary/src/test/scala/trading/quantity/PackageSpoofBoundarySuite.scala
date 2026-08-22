package trading.quantity

import scala.compiletime.testing.typeCheckErrors

import munit.FunSuite

class PackageSpoofBoundarySuite extends FunSuite:

  inline def assertRejected(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.isEmpty then
      fail("expected downstream package-spoofing source to be rejected")

  inline def assertAccepted(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.nonEmpty then
      fail(s"expected downstream prelude to compile: ${errors.map(_.message).mkString("; ")}")

  test("supported package-spoofing preludes compile before forbidden expressions"):
    assertAccepted:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("package-spoof-prelude"))
      val grid = UniformGrid.create(GridId("package-spoof-prelude"), GridVersion(1), dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val exact = Quantity(dimension.dimension, 1)
      val coordinate = grid.fromCoordinate(1)
      val positive = Positive(exact).toOption.get
      val nonzero = positive.asNonZero
      """

  test("core package spoofing cannot invoke lexical raw attachment"):
    assertRejected:
      """
      import trading.quantity.*
      val dimension = DimRef.atomic(AtomId("raw-quantity-attachment"))
      val forged: Quantity[dimension.D] = Quantity.fromCoefficient(Rational(99))
      """
    assertRejected:
      """
      import trading.quantity.*
      val dimension = DimRef.atomic(AtomId("raw-grid-attachment"))
      sealed trait ArbitraryGrid
      val forged: GridQuantity[dimension.D, ArbitraryGrid] =
        GridQuantity.fromCoordinate[dimension.D, ArbitraryGrid](BigInt(99))
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("raw-grid-inspection"))
      val grid = UniformGrid.create(GridId("raw-grid-inspection"), GridVersion(1), dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val value = grid.fromCoordinate(99)
      val forged = GridQuantity.coordinate(value)
      """

  test("core package spoofing cannot invoke lexical quantity result helpers"):
    assertRejected:
      """
      import trading.quantity.*
      val dimension = DimRef.atomic(AtomId("private-add"))
      val left = Quantity(dimension.dimension, 2)
      val right = Quantity(dimension.dimension, 3)
      val forged = Quantity.add(left, right)
      """
    assertRejected:
      """
      import trading.quantity.*
      val leftDimension = DimRef.atomic(AtomId("private-product-left"))
      val rightDimension = DimRef.atomic(AtomId("private-product-right"))
      val left = Quantity(leftDimension.dimension, 2)
      val right = Quantity(rightDimension.dimension, 3)
      val forged = Quantity.multiply(left, right)
      """
    assertRejected:
      """
      import trading.quantity.*
      val from = DimRef.atomic(AtomId("private-rate-from"))
      val to = DimRef.atomic(AtomId("private-rate-to"))
      val value = Quantity(from.dimension, 2)
      val rate = Rate(from.dimension, to.dimension, Rational(3))
      val forged = Quantity.convert(value, rate)
      """
    assertRejected:
      """
      import trading.quantity.*
      val first = DimRef.atomic(AtomId("private-compose-first"))
      val second = DimRef.atomic(AtomId("private-compose-second"))
      val third = DimRef.atomic(AtomId("private-compose-third"))
      val firstRate = Rate(first.dimension, second.dimension, Rational(2))
      val secondRate = Rate(second.dimension, third.dimension, Rational(3))
      val forged = Quantity.compose(firstRate, secondRate)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("private-division"))
      val value = Quantity(dimension.dimension, 6)
      val divisor = NonZero(value).toOption.get
      val forged = Quantity.divide(value, divisor)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("private-ratio"))
      val value = Quantity(dimension.dimension, 6)
      val divisor = NonZero(value).toOption.get
      val forged = Quantity.ratio(value, divisor)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("private-scalar-division"))
      val value = Quantity(dimension.dimension, 6)
      val divisor = NonZeroWhole(2).toOption.get
      val forged = Quantity.exactDivide(value, divisor)
      """

  test("core package spoofing cannot invoke lexical grid result helpers"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("private-grid-add"))
      val grid = UniformGrid.create(GridId("private-grid-add"), GridVersion(1), dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val left = grid.fromCoordinate(2)
      val right = grid.fromCoordinate(3)
      val forged = GridQuantity.add(left, right)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("private-grid-scale"))
      val grid = UniformGrid.create(GridId("private-grid-scale"), GridVersion(1), dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val value = grid.fromCoordinate(2)
      val forged = GridQuantity.scale(value, BigInt(3))
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("private-grid-quot-rem"))
      val grid = UniformGrid.create(GridId("private-grid-quot-rem"), GridVersion(1), dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val value = grid.fromCoordinate(7)
      val divisor = PositiveWhole(2).toOption.get
      val forged = GridQuantity.quotRem(value, divisor)
      """

  test("core package spoofing cannot implement witnesses or evidence"):
    assertRejected:
      """
      import trading.quantity.*
      val forged = new DimRef[One]:
        val key = DimensionKey.one
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged = new GridRef[One]:
        type G = this.type
        val id = GridId("forged")
        val version = GridVersion(1)
        val dimension = DimRef.one
        val quantum = PositiveRational.exact(1, 100).toOption.get
      """
    assertRejected:
      """
      import trading.quantity.*
      val forged = new SameDimension[One, One] {}
      """

  test("core package spoofing cannot implement the closed grammar or removed normalization proof"):
    assertRejected:
      """
      import trading.quantity.*
      val forged = new Power["package-spoof", 1] {}
      """
    assertRejected:
      """
      import trading.quantity.*
      type Entry = Power["package-spoof", 1]
      val forged = new Dim[Entry *: EmptyTuple] {}
      """
    assertRejected:
      """
      import trading.quantity.*
      val forged = new Normalize[One]:
        type Out = One
      """

  test("core package spoofing cannot extend the closed dimension universe"):
    assertRejected:
      """
      import trading.quantity.*
      val forged = new Dimension {}
      """

  test("removed associated-output normalization cannot be named"):
    assertRejected:
      """
      import trading.quantity.*
      type A = Atom["selected-output"]
      type Wrong = Dim[Power["selected-output", 2] *: EmptyTuple]
      val forged: Normalize.Aux[A, Wrong] = Normalize.derived[A]
      """

  test("core package spoofing cannot forge normalized results or obtain unequal conversions"):
    assertRejected:
      """
      import trading.quantity.*
      type Normalized = Dim[Power["normalized", 1] *: EmptyTuple]
      val forged: Quantity[Normalized] = Rational.one
      """
    assertRejected:
      """
      import trading.quantity.*
      type A = Atom["conversion:A"]
      type B = Atom["conversion:B"]
      summon[Conversion[Quantity[A], Quantity[B]]]
      """

  test("core package spoofing cannot invoke refinement evidence constructors"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged = new Sign[Rational](_ => 1)
      """

  test("core package spoofing cannot construct opaque quantity carriers directly"):
    assertRejected:
      """
      import trading.quantity.*
      val forged: Quantity[One] = Rational.one
      """
    assertRejected:
      """
      import trading.quantity.*
      sealed trait GridTag
      val forged: GridQuantity[One, GridTag] = BigInt(1)
      """

  test("core package spoofing cannot construct scalar and quantity refinements directly"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged: PositiveRational = Rational(-1)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged: PositiveWhole = BigInt(0)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged: NonZeroWhole = BigInt(0)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged: PositiveInt = 0
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
      val raw = Quantity(DimRef.one, 0)
      val forged: Positive[Quantity[One]] = raw
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val raw = Quantity(DimRef.one, 0)
      val forged: NonZero[Quantity[One]] = raw
      """

  test("all supported algebra imports expose no Typelevel Field for Rational"):
    assertRejected:
      """
      import algebra.ring.Field
      import trading.quantity.algebra.exactQuantityAlgebra.given
      import trading.quantity.Rational
      val field = summon[Field[Rational]]
      """

end PackageSpoofBoundarySuite
