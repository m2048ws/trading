package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.order.*
import trading.scenario.*

object AssociatedEvidenceShapes:
  val fixed         = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price99)
  val fixedOrder    = Order.stopMarket(instrument)(Side.Buy, lots, fixed).toOption.get
  val fixedEvidence = fixed.evidence(price100).toOption.get
  val trailing = TrailingActivation
    .create[B, Q](PriceReference.Mark, TriggerComparison.AtOrBelow, 1)
    .toOption
    .get
  val trailingOrder    = Order.stopMarket(instrument)(Side.Buy, lots, trailing).toOption.get
  val trailingEvidence = trailing.evidence(price100, price99).toOption.get
  val directLimit      = Order.limit(instrument)(Side.Buy, lots, price100).toOption.get
  val peg              = PeggedPricing[B, Q](PriceReference.Mark, 1)
  val peggedExecution = PricedExecution[D, B, Q, PeggedPricing[B, Q]](
    peg,
    TimeInForce.Day,
    LiquidityConstraint.Unrestricted,
    DisplayedVisibility
  )
  val peggedOrder = Order
    .create(instrument)(
      OrderIntent.create(instrument)(Side.Buy, lots).toOption.get,
      ImmediateActivation[B, Q](),
      peggedExecution
    )
    .toOption
    .get
  val pegResolution = peg.resolution(price99, price100).toOption.get

  val fixedAssumptions = ScenarioAssumptions.one(fixedOrder)(
    fixedEvidence,
    fixedOrder.execution.resolution,
    slice
  )
  val trailingAssumptions = ScenarioAssumptions.one(trailingOrder)(
    trailingEvidence,
    trailingOrder.execution.resolution,
    slice
  )
  val peggedAssumptions = ScenarioAssumptions.one(peggedOrder)(
    peggedOrder.activation.evidence,
    pegResolution,
    slice
  )

  // OFFENDING-BEGIN
  val fixedOnImmediate = ScenarioAssumptions.one(marketOrder)(
    fixedEvidence,
    marketOrder.execution.resolution,
    slice
  )
  val missingFixed = ScenarioAssumptions.one(fixedOrder)(
    marketOrder.activation.evidence,
    fixedOrder.execution.resolution,
    slice
  )
  val trailingOnFixed = ScenarioAssumptions.one(fixedOrder)(
    trailingEvidence,
    fixedOrder.execution.resolution,
    slice
  )
  val pegOnDirect = ScenarioAssumptions.one(directLimit)(
    directLimit.activation.evidence,
    pegResolution,
    slice
  )
  val directOnPegged = ScenarioAssumptions.one(peggedOrder)(
    peggedOrder.activation.evidence,
    directLimit.execution.pricing.resolution,
    slice
  )
  val immediateStop = Order.stopMarket(instrument)(
    Side.Buy,
    lots,
    ImmediateActivation[B, Q](),
    PositionEffect.Unrestricted
  )
  val untypedMaps = ScenarioAssumptions.one(marketOrder)(
    Map.empty[String, BigInt],
    Map.empty[String, BigInt],
    slice
  )
  // OFFENDING-END
end AssociatedEvidenceShapes
