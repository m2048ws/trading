package external

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class QuantityPublicTypeShapeSuite extends FunSuite:

  test("supported public type shapes compile"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)
      val nonNegative: NonNegative[Quantity[D]] = NonNegative(quantity).toOption.get
      val nonZero: NonZero[Quantity[D]] = NonZero(quantity).toOption.get
      val positive: Positive[Quantity[D]] = Positive(quantity).toOption.get
      """

  test("unsupported parameterized Quantity shape with rational carrier"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
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
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: Quantity[D, Rational] = ???
      """,
      "Too many type arguments for trading.quantity.Quantity"
    )

  test("unsupported parameterized Quantity shape with integer carrier"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
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
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: Quantity[D, BigInt] = ???
      """,
      "Too many type arguments for trading.quantity.Quantity"
    )

  test("unsupported parameterized Rate shape"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
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
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: Rate[A, B, Rational] = ???
      """,
      "Too many type arguments for trading.quantity.Rate"
    )

  test("unsupported parameterized Ratio shape"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
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
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: Ratio[Rational] = ???
      """,
      "trading.quantity.Ratio does not take type parameters"
    )

  test("unsupported alternate exact-quantity name"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val firstDimension = DimRef.atomic(AtomId("public-shape-first"))
      val secondDimension = DimRef.atomic(AtomId("public-shape-second"))
      type D = firstDimension.D
      type A = firstDimension.D
      type B = secondDimension.D
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
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
      val grid = UniformGrid.create(
        GridId("public-shape-grid"), GridVersion(1), firstDimension.dimension,
        PositiveRational.exact(1, 100).toOption.get
      )
      type G = grid.G

      val quantity: Quantity[D] = Quantity(firstDimension.dimension, BigInt(1))
      val gridQuantity: GridQuantity[D, G] = grid.fromCoordinate(BigInt(1))
      val rate: Rate[A, B] = Rate(firstDimension.dimension, secondDimension.dimension, Rational.one)
      val ratio: Ratio = Quantity(DimRef.one, Rational.one)

      val unsupported: RationalQuantity[D] = ???
      """,
      "Not found: type RationalQuantity"
    )

end QuantityPublicTypeShapeSuite
