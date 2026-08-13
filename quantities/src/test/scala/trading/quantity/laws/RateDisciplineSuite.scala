package trading.quantity.laws

import trading.quantity.AssetId
import trading.quantity.testkit.ExactGenerators.given
import trading.quantity.testkit.TestAsset

class RateDisciplineSuite extends TradingDisciplineSuite:
  private val first  = TestAsset.runtime(AssetId("rate-first"))
  private val second = TestAsset.runtime(AssetId("rate-second"))
  private val third  = TestAsset.runtime(AssetId("rate-third"))
  private val fourth = TestAsset.runtime(AssetId("rate-fourth"))

  checkAll(
    "Rate.categoryShape",
    new RateLaws(first.dimension, second.dimension, third.dimension, fourth.dimension).categoryShape
  )

end RateDisciplineSuite
