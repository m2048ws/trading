package external.fee.positive

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.fee.*
import trading.fee.policy.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.NonNegative
import trading.scenario.*

object FeePolicyBoundaryClient:
  val denomination = feePolicy
    .denomination(quote)(quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get
  val fee = feePolicy
    .percentage(
      denomination,
      FeeKind.from("completed-jar").toOption.get,
      NonNegative(Quantity(quote.dimension.ref, Rational(10))).toOption.get,
      FeeRate(Rational(1, 1000))
    )
    .toOption
    .get
  val assumptions = ScenarioAssumptions.one(marketOrder)(
    marketOrder.activation.evidence,
    marketOrder.execution.resolution,
    slice
  ).toOption.get
  val scenario = OrderScenario.evaluate(instrument)(assumptions).toOption.get
  val directive = FeeDirective(fee, SliceIndex.zero)
  val noFees    = FeePolicy.noFees(instrument)

  assert(directive.fee.amount.coefficient == Rational(-1, 100))
  assert(directive.sourceSlice.value == 0)
  assert(noFees.evaluate(scenario).contains(Vector.empty))
end FeePolicyBoundaryClient
