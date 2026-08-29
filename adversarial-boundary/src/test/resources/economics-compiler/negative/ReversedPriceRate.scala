package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.Price
import trading.quantity.*

object ReversedPriceRate:
  val reversed = Rate(quote.dimension.ref, base.dimension.ref, Rational.one)

  // OFFENDING-BEGIN
  val price = Price.fromRate(instrument)(reversed)
  // OFFENDING-END
end ReversedPriceRate
