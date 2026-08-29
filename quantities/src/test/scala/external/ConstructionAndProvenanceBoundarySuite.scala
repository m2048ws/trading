package external

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class ConstructionAndProvenanceBoundarySuite extends FunSuite:
  test("external Scala callers cannot invoke raw reconstruction helpers"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val forged: Quantity[One] = Quantity.fromCoefficient(Rational(99))
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val forged: GridQuantity[One, Unit] = GridQuantity.fromCoordinate(BigInt(99))
      """

  test("operation-specific implementation helpers remain hidden from callers"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("supported-helper-prelude"))
      val grid = UniformGrid.create(dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val exact = Quantity(dimension.dimension, 2)
      val coordinate = grid.fromCoordinate(3)
      val divisor = NonZero(exact).toOption.get
      val quotient = exact.divideBy(divisor)
      val embedded = grid.asQuantity(coordinate)
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val dimension = DimRef.atomic(AtomId("computed-quantity"))
      val left = Quantity(dimension.dimension, 2)
      val right = Quantity(dimension.dimension, 3)
      val forged = Quantity.add(left, right)
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("computed-grid"))
      val grid = UniformGrid.create(dimension.dimension,
        PositiveRational.exact(1, 100).toOption.get)
      val left = grid.fromCoordinate(2)
      val right = grid.fromCoordinate(3)
      val forged = GridQuantity.add(left, right)
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val leftDimension = DimRef.atomic(AtomId("computed-product-left"))
      val rightDimension = DimRef.atomic(AtomId("computed-product-right"))
      val left = Quantity(leftDimension.dimension, 2)
      val right = Quantity(rightDimension.dimension, 3)
      val forged = Quantity.multiply(left, right)
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val from = DimRef.atomic(AtomId("computed-rate-from"))
      val to = DimRef.atomic(AtomId("computed-rate-to"))
      val value = Quantity(from.dimension, 2)
      val rate = Rate(from.dimension, to.dimension, Rational(3))
      val forged = Quantity.convert(value, rate)
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val first = DimRef.atomic(AtomId("computed-compose-first"))
      val second = DimRef.atomic(AtomId("computed-compose-second"))
      val third = DimRef.atomic(AtomId("computed-compose-third"))
      val firstRate = Rate(first.dimension, second.dimension, Rational(2))
      val secondRate = Rate(second.dimension, third.dimension, Rational(3))
      val forged = Quantity.compose(firstRate, secondRate)
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("computed-division"))
      val value = Quantity(dimension.dimension, 2)
      val divisor = NonZero(value).toOption.get
      val forged = Quantity.divide(value, divisor)
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("computed-ratio"))
      val value = Quantity(dimension.dimension, 2)
      val divisor = NonZero(value).toOption.get
      val forged = Quantity.ratio(value, divisor)
      """

end ConstructionAndProvenanceBoundarySuite
