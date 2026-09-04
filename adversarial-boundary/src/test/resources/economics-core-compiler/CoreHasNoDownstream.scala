package external.economics.core

import trading.economics.instrument.Instrument

object CoreHasNoDownstream:
  val coreType: Class[Instrument] = classOf[Instrument]

  // OFFENDING-BEGIN
  val side: trading.order.Side = trading.order.Side.Buy
  object MissingExecution:
    import trading.execution.*
  val role: trading.scenario.LiquidityRole = trading.scenario.LiquidityRole.Taker
  object MissingCampaign:
    import trading.campaign.*
  val policy: Class[trading.fee.FeePolicy[?, ?, ?, ?, ?]] = classOf[trading.fee.FeePolicy[?, ?, ?, ?, ?]]
  val risk: Class[trading.risk.RiskIdentityError] = classOf[trading.risk.RiskIdentityError]
  object MissingApplication:
    import trading.application.*
  object MissingRuntime:
    import trading.runtime.*
  object MissingBoundaryCodecs:
    import trading.codec.*
  // OFFENDING-END
end CoreHasNoDownstream
