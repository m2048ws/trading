package external.fee.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.fee.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.scenario.*

object ForgedAssessment:
  val assumptions = ScenarioAssumptions.one(marketOrder)(
    marketOrder.activation.evidence,
    marketOrder.execution.resolution,
    slice
  ).toOption.get
  val scenario = OrderScenario.evaluate(instrument)(assumptions).toOption.get
  val denomination = FeeDenomination
    .create(instrument)(quote, quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get
  val fee = Fee
    .create(instrument)(
      denomination,
      FeeKind.from("forged-assessment").toOption.get,
      Quantity(quote.dimension.ref, Rational(-1, 100))
    )
    .toOption
    .get

  // OFFENDING-BEGIN
  val forgedScenarioFees = new ScenarioFees[D, B, Q, S](scenario, Vector.empty)
  val forgedAssessedFee   = new AssessedFeeValue[Q, D, B, Q, S](fee, SliceIndex.zero, slice)
  val forgedAttributed = new AttributedFeeContributionValue[Q, D, B, Q, S](
    RoundTripLeg.Entry,
    0,
    ???,
    ???
  )
  val forgedInclusive = new FeeInclusivePnl[D, B, Q, S](???, ???, ???, Vector.empty, ???)
  // OFFENDING-END
end ForgedAssessment
