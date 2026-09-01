package external.fee.positive

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.fee.*
import trading.order.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.NonNegative
import trading.scenario.*

object FeePolicyBoundaryClient:
  val denomination = FeeDenomination
    .create(instrument)(quote, quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get
  val fee = Fee
    .create(instrument)(
      denomination,
      FeeKind.from("completed-jar").toOption.get,
      FeeCalculation.percentage(
        NonNegative(Quantity(quote.dimension.ref, Rational(10))).toOption.get,
        FeeRate(Rational(1, 1000))
      )
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
  val strategy = new FeePolicy[Nothing, D, B, Q, S]:
    val instrumentId: InstrumentId = instrument.identity.id
    def evaluate(value: OrderScenario[D, B, Q, MarketState[B, Q, S]]) =
      Right(Vector(directive))
  val assessed = FeeAssessment.evaluate(instrument)(scenario, strategy).toOption.get
  val exitState = MarketState.quoteSettled(instrument)(Price.exact(instrument)(Rational(101)).toOption.get).toOption.get
  val exitOrder = Order.market(instrument)(Side.Sell, lots).toOption.get
  val exitSlice = LiquiditySlice.create(instrument)(lots, exitState, LiquidityRole.Taker).toOption.get
  val exitAssumptions = ScenarioAssumptions.one(exitOrder)(
    exitOrder.activation.evidence,
    exitOrder.execution.resolution,
    exitSlice
  ).toOption.get
  val exitScenario = OrderScenario.evaluate(instrument)(exitAssumptions).toOption.get
  val roundTrip    = RoundTripScenario.create(instrument)(scenario, exitScenario).toOption.get
  val policies     = RoundTripFeePolicies.same(strategy)
  val inclusivePnl = FeeInclusivePnl.evaluate(instrument)(roundTrip, policies).toOption.get

  assert(directive.fee.amount.coefficient == Rational(-1, 100))
  assert(directive.sourceSlice.value == 0)
  assert(noFees.evaluate(scenario).contains(Vector.empty))
  assert(assessed.scenario eq scenario)
  assert(assessed.fees.head.sourceSlice eq slice)
  assert(inclusivePnl.roundTrip eq roundTrip)
  assert(inclusivePnl.attributedContributions.map(_.leg) == Vector(RoundTripLeg.Entry, RoundTripLeg.Exit))
  assert(inclusivePnl.pricePnl.quantity.coefficient == Rational(2))
  assert(inclusivePnl.feePnl.coefficient == Rational(-1, 50))
  assert(inclusivePnl.netPnl.coefficient == Rational(99, 50))
end FeePolicyBoundaryClient
