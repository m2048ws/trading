package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.Side

object AssociatedEvidenceShapes:
  // OFFENDING-BEGIN
  val fixedOnImmediate = instrument.scenarios.assumptionsOne(marketOrder)(
    fixedEvidence,
    marketOrder.execution.resolution,
    slice
  )
  val missingFixed = instrument.scenarios.assumptionsOne(fixedOrder)(
    marketOrder.activation.evidence,
    fixedOrder.execution.resolution,
    slice
  )
  val trailingOnFixed = instrument.scenarios.assumptionsOne(fixedOrder)(
    trailingEvidence,
    fixedOrder.execution.resolution,
    slice
  )
  val pegOnDirect = instrument.scenarios.assumptionsOne(directLimit)(
    directLimit.activation.evidence,
    pegResolution,
    slice
  )
  val directOnPegged = instrument.scenarios.assumptionsOne(peggedOrder)(
    peggedOrder.activation.evidence,
    directLimit.execution.pricing.resolution,
    slice
  )
  val immediateStop = instrument.orders.stopMarket(Side.Buy, lots, instrument.orders.immediate)
  // OFFENDING-END

end AssociatedEvidenceShapes
