package trading.quantity.laws

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.given
import trading.quantity.testkit.TestAsset

class RefinementDisciplineSuite extends TradingDisciplineSuite:
  private val asset = TestAsset.runtime(AssetId("refinement-discipline"))
  private val grid  = UniformGrid.create(
    GridId("refinement-discipline-grid"),
    GridVersion(1),
    asset.dimension,
    PositiveRational.exact(1, 100).toOption.get
  )

  checkAll(
    "Refinement.latticeAndClosure",
    new RefinementLaws(grid).refinements
  )

end RefinementDisciplineSuite
