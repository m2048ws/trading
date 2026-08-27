package external.economics.negative

import trading.economics.*

object RefinementLoss:
  def reject(
    instrument: Instrument,
    lots: instrument.Lots,
    price: instrument.Price
  ): Unit =
    val _ = lots.count
    val _ = price.ticks

    // OFFENDING-BEGIN
    val _: instrument.Lots = instrument.flatPosition
    val _: instrument.Lots = instrument.positionLots(Side.Buy, lots)
    val _: instrument.Lots = lots.quantity - lots.quantity
    val _: instrument.Price = price.rate - price.rate
    val _: instrument.Price = -price.rate
    // OFFENDING-END

end RefinementLoss
