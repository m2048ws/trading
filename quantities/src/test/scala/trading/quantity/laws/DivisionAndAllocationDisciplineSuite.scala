package trading.quantity.laws

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.TestAsset

class DivisionAndAllocationDisciplineSuite extends TradingDisciplineSuite:
  private val asset = TestAsset.runtime(AtomId("division-allocation-discipline"))
  private val grid  = UniformGrid.create(asset.dimension,
    PositiveRational.exact(1, 100).toOption.get
  )

  checkAll(
    "GridQuantity.divisionAndAllocation",
    new DivisionAndAllocationLaws(grid).divisionAndAllocation
  )

end DivisionAndAllocationDisciplineSuite
