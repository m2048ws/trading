package external

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class GridQuantityPublicTypeShapeSuite extends FunSuite:

  test("unsupported coordinate abstraction"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)
      """

    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: CoordinateGrid[GridQuantity[D, G]] = ???
      """,
      "Not found: type CoordinateGrid"
    )

  test("unsupported parameterized coordinate abstraction"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)
      """

    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: CoordinateGrid[GridQuantity[D, G], BigInt] = ???
      """,
      "Not found: type CoordinateGrid"
    )

  test("unsupported coordinate representation name"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)
      """

    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: Quanta[G] = ???
      """,
      "Not found: type Quanta"
    )

  test("unsupported separate divisor carrier"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)
      """

    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: NonZeroDivisor[D] = ???
      """,
      "Not found: type NonZeroDivisor"
    )

end GridQuantityPublicTypeShapeSuite
