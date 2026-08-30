package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*

object RawCoreConstruction:
  val observed = instrument.identity.id

  // OFFENDING-BEGIN
  val rawLots = new Lots(instrument.identity.id, lots.count, instrument.positionLotGrid, lots.quantity)
  val rawPosition = new PositionLots(instrument.identity.id, 0, instrument.positionLotGrid, lots.quantity)
  val rawPrice = new Price(instrument.identity.id, price100.ticks, instrument.priceGrid, price100.rate)
  // OFFENDING-END
end RawCoreConstruction
