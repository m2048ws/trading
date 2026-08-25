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
    firstScenario: first.OrderScenario,
    secondScenario: second.OrderScenario,
    firstRoundTrip: first.RoundTripScenario,
    secondRoundTrip: second.RoundTripScenario,
    firstSchedule: first.FeeSchedule,
    secondSchedule: second.FeeSchedule
  ): Unit =
    val _ = first.lotCount(firstLots)
    val _ = first.prices.ticks(firstPrice)
    val _ = first.valuation.pnl(firstRoundTrip, firstSchedule)

    // OFFENDING-BEGIN
    val _ = first.lotCount(secondLots)
    val _: first.Prices = second.prices
    val _: first.Orders = second.orders
    val _ = first.prices.ticks(secondPrice)
    val _ = first.scenarios.order(firstScenario.order, secondScenario.slices)
    val _ = first.valuation.pnl(secondRoundTrip, firstSchedule)
    val _ = first.valuation.pnl(firstRoundTrip, secondSchedule)
    val _ = first.sizing.maxLots(
      Quantity(first.settle.dimension.asDimensionRef, Rational.one),
      PositiveWhole(1).toOption.get,
      firstSchedule
    )(_ => Right(secondRoundTrip))
    // OFFENDING-END

end CrossInstrumentMixing
