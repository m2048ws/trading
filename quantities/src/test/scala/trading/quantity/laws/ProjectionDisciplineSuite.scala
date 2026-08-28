package trading.quantity.laws

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.given
import trading.quantity.testkit.TestAsset

class ProjectionDisciplineSuite extends TradingDisciplineSuite:
  private val asset = TestAsset.runtime(AtomId("projection-discipline"))

  private val cent = UniformGrid.create(asset.dimension,
    PositiveRational.exact(1, 100).toOption.get
  )
  private val satoshi = UniformGrid.create(asset.dimension,
    PositiveRational.exact(1, 100000000).toOption.get
  )
  private val threeHundredths = UniformGrid.create(asset.dimension,
    PositiveRational.exact(3, 100).toOption.get
  )
  private val twoFifteenths = UniformGrid.create(asset.dimension,
    PositiveRational.exact(2, 15).toOption.get
  )

  checkAll("Projection.cent.narrowing", new ExactNarrowingLaws(cent).partialIsomorphism)
  checkAll("Projection.cent.quantization", new QuantizationLaws(cent).quantization)
  checkAll("Projection.satoshi.narrowing", new ExactNarrowingLaws(satoshi).partialIsomorphism)
  checkAll("Projection.satoshi.quantization", new QuantizationLaws(satoshi).quantization)
  checkAll(
    "Projection.threeHundredths.narrowing",
    new ExactNarrowingLaws(threeHundredths).partialIsomorphism
  )
  checkAll(
    "Projection.threeHundredths.quantization",
    new QuantizationLaws(threeHundredths).quantization
  )
  checkAll(
    "Projection.twoFifteenths.narrowing",
    new ExactNarrowingLaws(twoFifteenths).partialIsomorphism
  )
  checkAll(
    "Projection.twoFifteenths.quantization",
    new QuantizationLaws(twoFifteenths).quantization
  )

end ProjectionDisciplineSuite
