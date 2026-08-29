package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.quantity.*

object ConversionDoesNotGrantGrid:
  val exact = state
    .convertToSettle(base)(Quantity(base.dimension.ref, Rational.one))
    .toOption
    .get

  // OFFENDING-BEGIN
  val coordinate: GridQuantity[quote.D, settleGrid.G] = exact
  // OFFENDING-END

end ConversionDoesNotGrantGrid
