package trading.quantity.laws

import algebra.laws.RingLaws
import org.typelevel.discipline.Predicate

import trading.quantity.DimKey
import trading.quantity.algebra.dimensionAlgebra.given
import trading.quantity.testkit.ExactGenerators.given

class DimensionDisciplineSuite extends TradingDisciplineSuite:

  private val everyDimension = new Predicate[DimKey]:
    def apply(v: DimKey): Boolean = true

  checkAll(
    "DimKey.multiplicativeCommutativeGroup",
    RingLaws
      .withPred[DimKey](everyDimension)
      .multiplicativeCommutativeGroup
  )

  checkAll(
    "DimKey.normalization",
    new DimensionNormalizationLaws().normalization
  )

end DimensionDisciplineSuite
