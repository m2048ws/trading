package external.economics.negative

import trading.economics.*
import trading.quantity.*
import trading.quantity.refinement.PositiveWhole

object RemovedFlatApi:
  def reject(
    instrument: Instrument,
    price: instrument.Price,
    lots: instrument.Lots,
    state: instrument.MarketState,
    order: instrument.Order,
    scenario: instrument.OrderScenario,
    roundTrip: instrument.RoundTripScenario,
    schedule: instrument.FeeSchedule
  ): Unit =
    val _ = price.ticks
    val _ = instrument.orders.market(Side.Buy, lots)
    val _ = instrument.valuation.pnl(roundTrip, schedule)

    // OFFENDING-BEGIN
    val _ = instrument.price(1)
    val _ = instrument.priceExactly(price.rate)
    val _ = instrument.marketStateForQuote(price)
    val _ = instrument.marketOrder(Side.Buy, lots)
    val _ = instrument.positionValue(instrument.positionLots(Side.Buy, lots), state)
    val _ = instrument.calculatePnl(roundTrip, schedule)
    val _ = instrument.lotCount(lots)
    val _ = order.kind
    val _ = scenario.activationEvidence
    val _ = instrument.sizePosition(
      Quantity(instrument.roles.settle.dimension.asDimensionRef, Rational.one),
      PositiveWhole(1).toOption.get,
      schedule
    )(_ => Right(roundTrip))
    // OFFENDING-END

end RemovedFlatApi
