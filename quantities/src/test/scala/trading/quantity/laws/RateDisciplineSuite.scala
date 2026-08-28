package trading.quantity.laws

import trading.quantity.AtomId
import trading.quantity.testkit.ExactGenerators.given
import trading.quantity.testkit.TestAsset

class RateDisciplineSuite extends TradingDisciplineSuite:
  private val first  = TestAsset.runtime(AtomId("rate-first"))
  private val second = TestAsset.runtime(AtomId("rate-second"))
  private val third  = TestAsset.runtime(AtomId("rate-third"))
  private val fourth = TestAsset.runtime(AtomId("rate-fourth"))

  checkAll(
    "Rate.categoryShape",
    new RateLaws(first.dimension, second.dimension, third.dimension, fourth.dimension).categoryShape
  )

end RateDisciplineSuite
