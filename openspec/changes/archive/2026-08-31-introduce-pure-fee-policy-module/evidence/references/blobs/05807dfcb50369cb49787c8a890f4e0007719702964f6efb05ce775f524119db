package external.fee.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.fee.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.NonNegative

object RawFeeBasis:
  val denomination = feePolicy
    .denomination(quote)(quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get
  val kind     = FeeKind.from("raw-basis").toOption.get
  val rawBasis = Quantity(quote.dimension.ref, Rational(10))
  val rate     = FeeRate(Rational(1, 1000))
  val basis    = NonNegative(rawBasis).toOption.get
  val rawRate  = Rational(1, 1000)

  // OFFENDING-BEGIN
  val rejectedBasis = feePolicy.percentage(denomination, kind, rawBasis, rate)
  val rejectedRate  = FeeCalculation.percentage(basis, rawRate)
  // OFFENDING-END
end RawFeeBasis
