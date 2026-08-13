package external

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class GridOperationImportBoundarySuite extends FunSuite:

  test("core grid construction and arithmetic need only the core wildcard import"):
    assertCompiles:
      """
      import trading.quantity.*

      val quantum = trading.quantity.refinement.PositiveRational.exact(1, 100).toOption.get
      val grid = UniformGrid.create(GridId("core-only-grid"), GridVersion(1), DimRef.one, quantum)
      val value = grid.fromCoordinate(7)
      val sum: GridQuantity[One, grid.G] = value + value
      val difference: GridQuantity[One, grid.G] = value - value
      val scaled: GridQuantity[One, grid.G] = value * BigInt(3)
      val exact: Quantity[One] = value.asQuantity(grid)
      """

  test("quotRemBy is unavailable from the core wildcard import alone"):
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*

      def divide[D <: Dimension, G](
        value: GridQuantity[D, G],
        divisor: trading.quantity.refinement.PositiveWhole,
        grid: GridRef.Grid[D, G]
      ) = value.quotRemBy(divisor, grid)
      """,
      "value quotRemBy is not a member"
    )

  test("allocateEvenly is unavailable from the core wildcard import alone"):
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*

      def allocate[D <: Dimension, G](
        value: GridQuantity[D, G],
        count: trading.quantity.refinement.PositiveInt,
        order: trading.quantity.grid.RemainderOrder,
        grid: GridRef.Grid[D, G]
      ) = value.allocateEvenly(count, order, grid)
      """,
      "value allocateEvenly is not a member"
    )

  test("grid wildcard import supplies quotient/remainder and allocation extensions"):
    assertCompiles:
      """
      import trading.quantity.*
      import trading.quantity.grid.*

      val quantum = trading.quantity.refinement.PositiveRational.exact(1, 100).toOption.get
      val divisor = trading.quantity.refinement.PositiveWhole(3).toOption.get
      val count = trading.quantity.refinement.PositiveInt(3).toOption.get
      val grid = UniformGrid.create(GridId("grid-operations-import"), GridVersion(1), DimRef.one, quantum)
      val value = grid.fromCoordinate(10)
      val divided: QuotRem[GridQuantity[One, grid.G]] = value.quotRemBy(divisor, grid)
      val allocated: Allocation[GridQuantity[One, grid.G]] =
        value.allocateEvenly(count, RemainderOrder.FirstToLast, grid)
      """

end GridOperationImportBoundarySuite
