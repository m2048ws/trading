package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.order.*

object AssociatedEvidenceShapes:
  val fixed = orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, price99)
  val fixedOrder = orders.stopMarket(Side.Buy, lots, fixed).toOption.get
  val fixedEvidence = orders.fixedEvidence(fixed)(price100).toOption.get
  val trailing = orders
    .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrBelow, 1)
    .toOption
    .get
  val trailingOrder = orders.stopMarket(Side.Buy, lots, trailing).toOption.get
  val trailingEvidence = orders.trailingEvidence(trailing)(price100, price99).toOption.get
  val directLimit = orders.limit(Side.Buy, lots, price100).toOption.get
  val peg         = orders.peggedPricing(PriceReference.Mark, 1)
  val peggedExecution = orders.pricedExecution(
    peg,
    TimeInForce.Day,
    LiquidityConstraint.Unrestricted,
    orders.displayed
  )
  val peggedOrder = orders
    .create(orders.intent(Side.Buy, lots), orders.immediate, peggedExecution)
    .toOption
    .get
  val pegResolution = orders.pegResolution(peg)(price99, price100).toOption.get

  // OFFENDING-BEGIN
  val fixedOnImmediate = scenarios.assumptionsOne(marketOrder)(
    fixedEvidence,
    marketOrder.execution.resolution,
    slice
  )
  val missingFixed = scenarios.assumptionsOne(fixedOrder)(
    marketOrder.activation.evidence,
    fixedOrder.execution.resolution,
    slice
  )
  val trailingOnFixed = scenarios.assumptionsOne(fixedOrder)(
    trailingEvidence,
    fixedOrder.execution.resolution,
    slice
  )
  val pegOnDirect = scenarios.assumptionsOne(directLimit)(
    directLimit.activation.evidence,
    pegResolution,
    slice
  )
  val directOnPegged = scenarios.assumptionsOne(peggedOrder)(
    peggedOrder.activation.evidence,
    directLimit.execution.pricing.resolution,
    slice
  )
  val immediateStop = orders.stopMarket(Side.Buy, lots, orders.immediate, PositionEffect.Unrestricted)
  // OFFENDING-END
end AssociatedEvidenceShapes
