package trading.quantity.laws

import algebra.laws.RingLaws
import cats.kernel.laws.discipline.OrderTests
import org.scalacheck.Arbitrary
import org.scalacheck.Cogen
import org.typelevel.discipline.Predicate

import trading.quantity.*
import trading.quantity.algebra.LeftModule
import trading.quantity.algebra.exactOrders.given
import trading.quantity.algebra.gridQuantityAlgebra.given
import trading.quantity.algebra.refinedAdditive.given
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators
import trading.quantity.testkit.TestAsset

class GridQuantityDisciplineSuite extends TradingDisciplineSuite:
  private val asset                  = TestAsset.runtime(AssetId("grid-discipline"))
  private val grid: GridRef[asset.D] = UniformGrid.create(
    GridId("grid-discipline-cent"),
    GridVersion(1),
    asset.dimension,
    PositiveRational.exact(1, 100).toOption.get
  )
  private given DimRef[asset.D] = asset.dimension

  private given Arbitrary[GridQuantity[asset.D, grid.G]] =
    ExactGenerators.arbitraryGridQuantity(grid)
  private given Cogen[GridQuantity[asset.D, grid.G]]                  = ExactGenerators.cogenGridQuantity(grid)
  private given Arbitrary[NonNegative[GridQuantity[asset.D, grid.G]]] =
    Arbitrary(ExactGenerators.nonNegativeGridQuantity(grid))
  private given Arbitrary[Positive[GridQuantity[asset.D, grid.G]]] =
    Arbitrary(ExactGenerators.positiveGridQuantity(grid))

  checkAll(
    "GridQuantity.additiveCommutativeGroup",
    RingLaws[GridQuantity[asset.D, grid.G]].additiveCommutativeGroup
  )

  checkAll(
    "GridQuantity.order",
    OrderTests[GridQuantity[asset.D, grid.G]].order
  )

  checkAll(
    "GridQuantity.leftModule",
    new LeftModuleLaws(summon[LeftModule[GridQuantity[asset.D, grid.G], BigInt]]).leftModule
  )

  checkAll(
    "GridQuantity.embedding",
    new GridEmbeddingLaws(grid).embedding
  )

  checkAll(
    "NonNegativeGridQuantity.additiveCommutativeMonoid",
    RingLaws[NonNegative[GridQuantity[asset.D, grid.G]]].additiveCommutativeMonoid
  )

  private val everyPositiveGrid = new Predicate[Positive[GridQuantity[asset.D, grid.G]]]:
    def apply(v: Positive[GridQuantity[asset.D, grid.G]]): Boolean = true

  checkAll(
    "PositiveGridQuantity.additiveCommutativeSemigroup",
    RingLaws
      .withPred[Positive[GridQuantity[asset.D, grid.G]]](everyPositiveGrid)
      .additiveCommutativeSemigroup
  )

end GridQuantityDisciplineSuite
