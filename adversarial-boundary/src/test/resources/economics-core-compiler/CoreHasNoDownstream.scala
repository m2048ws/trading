package external.economics.core

import trading.economics.instrument.Instrument

object CoreHasNoDownstream:
  val coreType: Class[Instrument] = classOf[Instrument]

  // OFFENDING-BEGIN
  val side: trading.order.Side = trading.order.Side.Buy
  val role: trading.scenario.LiquidityRole = trading.scenario.LiquidityRole.Taker
  val policy: Class[trading.fee.policy.FeePolicy[?]] = classOf[trading.fee.policy.FeePolicy[?]]
  val risk: Class[trading.risk.RiskIdentityError] = classOf[trading.risk.RiskIdentityError]
  // OFFENDING-END
end CoreHasNoDownstream
