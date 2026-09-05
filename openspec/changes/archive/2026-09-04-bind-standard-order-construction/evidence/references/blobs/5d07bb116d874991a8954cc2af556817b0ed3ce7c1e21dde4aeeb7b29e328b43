package external.order.positive

import trading.economics.instrument.*
import trading.order.*

object InstrumentOrderScopeClient:
  def standardOrders[I <: Instrument](instrument: I)(
    lots: instrument.Lots,
    price: instrument.Price
  ): Unit =
    val orders = Order.forInstrument(instrument)
    val trigger = FixedActivation(
      PriceReference.Mark,
      TriggerComparison.AtOrAbove,
      price
    )
    val visibility: PricedVisibility[instrument.PositionD] = IcebergVisibility(lots)

    val market: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        ImmediateActivation[instrument.BaseD, instrument.QuoteD],
        MarketExecution[instrument.PositionD, instrument.BaseD, instrument.QuoteD]
      ]
    ] = orders.market(Side.Buy, lots)

    val limit: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        ImmediateActivation[instrument.BaseD, instrument.QuoteD],
        PricedExecution[
          instrument.PositionD,
          instrument.BaseD,
          instrument.QuoteD,
          LimitPricing[instrument.BaseD, instrument.QuoteD]
        ]
      ]
    ] = orders.limit(
      Side.Sell,
      lots,
      price,
      TimeInForce.Day,
      LiquidityConstraint.MakerOnly,
      PositionEffect.ReduceOnly,
      visibility
    )

    val stopMarket: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        trigger.type,
        MarketExecution[instrument.PositionD, instrument.BaseD, instrument.QuoteD]
      ]
    ] = orders.stopMarket(Side.Buy, lots, trigger)

    val stopLimit: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        trigger.type,
        PricedExecution[
          instrument.PositionD,
          instrument.BaseD,
          instrument.QuoteD,
          LimitPricing[instrument.BaseD, instrument.QuoteD]
        ]
      ]
    ] = orders.stopLimit(Side.Sell, lots, trigger, price)

    val _ = (market, limit, stopMarket, stopLimit)
end InstrumentOrderScopeClient
