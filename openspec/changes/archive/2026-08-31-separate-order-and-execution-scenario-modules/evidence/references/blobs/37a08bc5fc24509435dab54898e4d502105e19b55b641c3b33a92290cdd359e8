package external.order.negative

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.Dim
import trading.quantity.refinement.PositiveWhole

object ImpossibleInstructionShapes:
  def rejected[D <: Dim, B <: Dim, Q <: Dim](
    intent: OrderIntent[D],
    lots: Lots[D],
    price: Price[B, Q],
    observed: Price[B, Q]
  ): Unit =
    val fixed = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price)
    val trailing = TrailingActivation
      .create[B, Q](PriceReference.Last, TriggerComparison.AtOrBelow, 1)
      .toOption
      .get
    val fixedEvidence    = fixed.evidence(observed).toOption.get
    val trailingEvidence = trailing.evidence(price, observed).toOption.get
    val limit            = LimitPricing(price)
    val peg              = PeggedPricing[B, Q](PriceReference.Mark, 1)
    val pegEvidence      = peg.resolution(price, observed).toOption.get

    // OFFENDING-BEGIN
    val rawTrailing = new TrailingActivation[B, Q](
      PriceReference.Mark,
      TriggerComparison.AtOrAbove,
      PositiveWhole(1).toOption.get
    )
    val restingMarket = MarketExecution[D, B, Q](TimeInForce.Day)
    val marketWithPricedState = MarketExecution[D, B, Q](
      NonRestingTimeInForce.ImmediateOrCancel,
      LiquidityConstraint.MakerOnly,
      DisplayedVisibility
    )
    val fixedWithTrailingEvidence = fixed.verify(trailingEvidence)
    val trailingWithFixedEvidence = trailing.verify(fixedEvidence)
    val directWithPegEvidence     = limit.resolve(pegEvidence)
    val pegWithDirectEvidence     = peg.resolve(DirectPricingResolution)
    val priceAsIcebergLots        = IcebergVisibility(price)
    val forgedIntent              = intent.copy(positionChange = intent.positionChange)
    val rawIntent = new OrderIntent[D](
      intent.instrumentId,
      intent.side,
      intent.lots,
      intent.positionEffect,
      intent.positionChange
    )
    // OFFENDING-END
    val _ = lots
end ImpossibleInstructionShapes
