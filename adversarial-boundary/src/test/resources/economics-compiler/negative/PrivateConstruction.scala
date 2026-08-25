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
    val _ = instrument.lotCount(lots)
    val _ = instrument.priceCoordinate(price)
    val _ = order.side
    val _ = scenario.slices
    val _ = fee.coordinate

    // OFFENDING-BEGIN
    val rawLots: GridQuantity[instrument.position.D, ?] = lots
    val rawPrice: Quantity[Divide[instrument.quote.D, instrument.base.D]] = price
    val forgedMarket = new instrument.MarketState:
      def price: instrument.Price = price
      def conversions: instrument.SettlementConversions = ???
    val forgedLine = new instrument.FeeLine:
      def fee: instrument.Fee = fee
      def sourceSliceIndex: Int = 0
      def sourceMarket: instrument.MarketState = forgedMarket
    // OFFENDING-END

end PrivateConstruction
