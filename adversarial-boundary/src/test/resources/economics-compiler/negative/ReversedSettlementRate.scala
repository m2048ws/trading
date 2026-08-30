package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.SettlementConversion
import trading.quantity.*

object ReversedSettlementRate:
  val reversed = Rate(quote.dimension.ref, base.dimension.ref, Rational.one)

  // OFFENDING-BEGIN
  val conversion = SettlementConversion.fromRate(instrument)(base)(reversed)
  // OFFENDING-END
end ReversedSettlementRate
