package external.scenario.negative

import trading.order.Side
import trading.scenario.*

object RemovedScenarioValuationApi:
  val side = Side.Buy

  // OFFENDING-BEGIN
  val rawSign = side.sign
  val oldLeg  = ScenarioLeg.Entry
  // OFFENDING-END
end RemovedScenarioValuationApi
