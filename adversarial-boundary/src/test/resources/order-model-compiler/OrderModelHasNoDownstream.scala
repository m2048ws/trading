package external.order.boundary

import trading.order.*

object OrderModelHasNoDownstream:
  val side: Side = Side.Buy

  // OFFENDING-BEGIN
  object MissingScenario:
    import trading.scenario.*

  object MissingFeePolicy:
    import trading.fee.*

  object MissingRisk:
    import trading.risk.*

  object MissingApplication:
    import trading.application.*

  object MissingRuntime:
    import trading.runtime.*

  object MissingBoundaryCodecs:
    import trading.codec.*

  val role: trading.scenario.LiquidityRole = trading.scenario.LiquidityRole.Maker
  // OFFENDING-END
