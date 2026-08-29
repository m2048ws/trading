package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*

object RefinementLoss:
  val observed = lots.count

  // OFFENDING-BEGIN
  val lotsFromQuantity: instrument.Lots = lots.quantity
  val lotsFromPosition: instrument.Lots = trading.economics.instrument.PositionLots.flat(instrument)
  val priceFromRate: instrument.Price = price100.rate
  val priceFromDifference: instrument.Price = price100.rate - price100.rate
  // OFFENDING-END
end RefinementLoss
