package external.order.boundary

import trading.order.*

object OrderModelHasNoDownstream:
  val side: Side = Side.Buy

  // OFFENDING-BEGIN
  import trading.scenario.*
  import trading.fee.policy.*
  import trading.risk.*
  import trading.application.*
  import trading.runtime.*

  val role: LiquidityRole = LiquidityRole.Maker
  // OFFENDING-END
