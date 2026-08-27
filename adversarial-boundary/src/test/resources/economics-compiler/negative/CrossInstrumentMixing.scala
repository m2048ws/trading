package external.economics.negative

import trading.economics.*
import trading.quantity.*
import trading.quantity.refinement.PositiveWhole

object CrossInstrumentMixing:
  def reject(
    first: Instrument,
    second: Instrument,
    firstLots: first.Lots,
    secondLots: second.Lots,
    firstPrice: first.Price,
    secondPrice: second.Price,
    firstConversion: first.SettleConversion,
    secondConversion: second.SettleConversion,
    firstOrder: first.Order,
    secondOrder: second.Order,
    firstScenario: first.OrderScenario,
    secondScenario: second.OrderScenario,
    firstRoundTrip: first.RoundTripScenario,
    secondRoundTrip: second.RoundTripScenario,
    firstSchedule: first.FeeSchedule,
    secondSchedule: second.FeeSchedule,
    firstDenomination: first.FeeDenomination,
    secondDenomination: second.FeeDenomination
  ): Unit =
    val _ = firstLots.count
    val _ = firstPrice.ticks
    val _ = first.valuation.pnl(firstRoundTrip, firstSchedule)

    // OFFENDING-BEGIN
    val _: first.Lots = secondLots
    val _: first.Price = secondPrice
    val _: first.SettleConversion = secondConversion
    val _: first.FeeDenomination = secondDenomination
    val _: first.Order = secondOrder
    val _: first.OrderScenario = secondScenario
    val _: first.Prices = second.prices
    val _: first.Orders = second.orders
    val _ = first.orders.market(Side.Buy, secondLots)
    val _ = first.scenarios.roundTrip(firstScenario, secondScenario)
    val _ = first.valuation.pnl(secondRoundTrip, firstSchedule)
    val _ = first.valuation.pnl(firstRoundTrip, secondSchedule)
    val _ = first.sizing.maxLots(
      Quantity(first.roles.settle.dimension.asDimensionRef, Rational.one),
      PositiveWhole(1).toOption.get,
      firstSchedule
    )(_ => Right(secondRoundTrip))
    // OFFENDING-END

end CrossInstrumentMixing
