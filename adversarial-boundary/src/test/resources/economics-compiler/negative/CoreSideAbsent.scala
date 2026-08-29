package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*

object CoreSideAbsent:
  val observed = instrument.identity.id

  // OFFENDING-BEGIN
  val side = trading.economics.instrument.Side.Buy
  val order: instrument.Order = marketOrder
  // OFFENDING-END
end CoreSideAbsent
