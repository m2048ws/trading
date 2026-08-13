package trading.quantity.laws

import algebra.laws.RingLaws
import org.typelevel.discipline.Predicate

import trading.quantity.DimensionKey
import trading.quantity.algebra.dimensionAlgebra.given
import trading.quantity.testkit.ExactGenerators.given

class DimensionDisciplineSuite extends TradingDisciplineSuite:

  private val everyDimension = new Predicate[DimensionKey]:
    def apply(v: DimensionKey): Boolean = true

  checkAll(
    "DimensionKey.multiplicativeCommutativeGroup",
    RingLaws
      .withPred[DimensionKey](everyDimension)
      .multiplicativeCommutativeGroup
  )

  checkAll(
    "DimensionKey.normalization",
    new DimensionNormalizationLaws().normalization
  )

end DimensionDisciplineSuite
