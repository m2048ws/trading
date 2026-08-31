package external.order.positive

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.Dim

object InstructionAlgebra:
  def allShapes[D <: Dim, B <: Dim, Q <: Dim](
    lots: Lots[D],
    trigger: Price[B, Q],
    observed: Price[B, Q],
    favorableExtreme: Price[B, Q]
  ): Unit =
    val immediate = ImmediateActivation[B, Q]()
    val fixed     = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, trigger)
    val trailing = TrailingActivation
      .create[B, Q](PriceReference.Last, TriggerComparison.AtOrBelow, 1)
      .toOption
      .get
    val fixedEvidence    = fixed.evidence(observed)
    val trailingEvidence = trailing.evidence(favorableExtreme, observed)
    val limit            = LimitPricing(trigger)
    val peg              = PeggedPricing[B, Q](PriceReference.Mark, 1)
    val pegResolution    = peg.resolution(trigger, observed)
    val marketIoc = MarketExecution[D, B, Q](NonRestingTimeInForce.ImmediateOrCancel)
    val marketFok = MarketExecution[D, B, Q](NonRestingTimeInForce.FillOrKill)
    val displayed: PricedVisibility[D] = DisplayedVisibility
    val hidden: PricedVisibility[D]    = HiddenVisibility
    val iceberg: PricedVisibility[D]   = IcebergVisibility(lots)
    val limitExecution = PricedExecution[D, B, Q, LimitPricing[B, Q]](
      limit,
      TimeInForce.GoodTillCancelled,
      LiquidityConstraint.Unrestricted,
      displayed
    )
    val pegExecution = PricedExecution[D, B, Q, PeggedPricing[B, Q]](
      peg,
      TimeInForce.Day,
      LiquidityConstraint.MakerOnly,
      hidden
    )
    val icebergExecution = PricedExecution[D, B, Q, LimitPricing[B, Q]](
      limit,
      TimeInForce.Day,
      LiquidityConstraint.Unrestricted,
      iceberg
    )
    val activations: Vector[OrderActivation[B, Q]] = Vector(immediate, fixed, trailing)
    val executions: Vector[OrderExecution[D, B, Q]] =
      Vector(marketIoc, marketFok, limitExecution, pegExecution, icebergExecution)
    val _ = (fixedEvidence, trailingEvidence, pegResolution, activations, executions)

  def activationName[B <: Dim, Q <: Dim](activation: OrderActivation[B, Q]): String =
    activation match
      case _: ImmediateActivation[?, ?] => "immediate"
      case FixedActivation(_, _, _)     => "fixed"
      case TrailingActivation(_, _, _)  => "trailing"

  def executionName[D <: Dim, B <: Dim, Q <: Dim](execution: OrderExecution[D, B, Q]): String =
    execution match
      case MarketExecution(_)                              => "market"
      case PricedExecution(LimitPricing(_), _, _, _)       => "limit"
      case PricedExecution(_: PeggedPricing[?, ?], _, _, _) => "peg"
end InstructionAlgebra
