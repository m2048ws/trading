package external.order.negative

import trading.economics.instrument.*
import trading.order.*

object InstrumentOrderScopeMismatch:
  def rejected[I <: Instrument, J <: Instrument](instrument: I, foreign: J)(
    lots: instrument.Lots,
    foreignLots: foreign.Lots,
    price: instrument.Price,
    foreignPrice: foreign.Price,
    trigger: TriggerActivation[instrument.BaseD, instrument.QuoteD],
    foreignTrigger: TriggerActivation[foreign.BaseD, foreign.QuoteD],
    foreignPeg: PeggedPricing[foreign.BaseD, foreign.QuoteD],
    foreignVisibility: PricedVisibility[foreign.PositionD]
  ): Unit =
    val orders = Order.forInstrument(instrument)
    val intent = OrderIntent.create(instrument)(Side.Buy, lots).toOption.get
    val immediate: ImmediateActivation[instrument.BaseD, instrument.QuoteD] =
      ImmediateActivation()
    val foreignExecution: PricedExecution[
      foreign.PositionD,
      foreign.BaseD,
      foreign.QuoteD,
      PeggedPricing[foreign.BaseD, foreign.QuoteD]
    ] = PricedExecution(
      foreignPeg,
      TimeInForce.Day,
      LiquidityConstraint.Unrestricted,
      foreignVisibility
    )

    // OFFENDING-BEGIN
    val wrongLots = orders.market(
      Side.Buy,
      foreignLots,
      PositionEffect.Unrestricted
    )
    val wrongPrice = orders.limit(
      Side.Buy,
      lots,
      foreignPrice,
      TimeInForce.Day,
      LiquidityConstraint.Unrestricted,
      PositionEffect.Unrestricted,
      DisplayedVisibility
    )
    val wrongTrigger = orders.stopMarket(
      Side.Buy,
      lots,
      foreignTrigger,
      PositionEffect.Unrestricted
    )
    val wrongVisibility = orders.stopLimit(
      Side.Buy,
      lots,
      trigger,
      price,
      TimeInForce.Day,
      LiquidityConstraint.Unrestricted,
      PositionEffect.Unrestricted,
      foreignVisibility
    )
    val wrongPeg = Order.create(instrument)(intent, immediate, foreignExecution)
    // OFFENDING-END
end InstrumentOrderScopeMismatch
