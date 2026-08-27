package external.economics.negative

import trading.economics.*
import trading.quantity.*

object PrivateConstruction:
  def validPrelude(
    instrument: Instrument,
    lots: instrument.Lots,
    price: instrument.Price,
    order: instrument.Order,
    scenario: instrument.OrderScenario,
    fee: instrument.Fee
  ): Unit =
    val _ = lots.count
    val _ = price.ticks
    val _ = order.intent.side
    val _ = scenario.assumptions.matchedSlices
    val _ = fee.coordinate

    // OFFENDING-BEGIN
    val rawLots: GridQuantity[instrument.roles.position.D, ?] = lots
    val rawPrice: Quantity[Divide[instrument.roles.quote.D, instrument.roles.base.D]] = price
    val forgedLots = new InstrumentLots[instrument.Owner, instrument.roles.position.D]:
      def count = lots.count
      def quantity = lots.quantity
    val forgedMarket = new InstrumentMarketState[
      instrument.Owner,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D]:
      def price = price
      def conversionSources = Vector.empty
      def baseToSettle = ???
      def quoteToSettle = ???
      def convertToSettle(source: trading.quantity.runtime.AssetRef)(value: Quantity[source.D]) = ???
    val forgedOrder = new InstrumentOrder[instrument.Owner, instrument.Lots, instrument.Price]:
      def intent = order.intent
      def activation = order.activation
      def execution = order.execution
    // OFFENDING-END

end PrivateConstruction
