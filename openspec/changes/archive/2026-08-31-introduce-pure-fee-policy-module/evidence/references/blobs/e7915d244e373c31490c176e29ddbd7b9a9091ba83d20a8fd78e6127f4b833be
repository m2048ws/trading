package external.fee.negative

import cats.kernel.Monoid

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.fee.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.scenario.*

sealed trait CustomPolicyFailure
case object CustomPolicyRejected extends CustomPolicyFailure

object UnlawfulPolicyApi:
  type Scenario = OrderScenario[D, B, Q, MarketState[B, Q, S]]
  type Policy[+E] = FeePolicy[E, D, B, Q, S]

  val assumptions = ScenarioAssumptions.one(marketOrder)(
    marketOrder.activation.evidence,
    marketOrder.execution.resolution,
    slice
  ).toOption.get
  val scenario = OrderScenario.evaluate(instrument)(assumptions).toOption.get
  val noFees   = FeePolicy.noFees(instrument)
  val failing = new Policy[CustomPolicyFailure]:
    val instrumentId: InstrumentId = instrument.identity.id
    def evaluate(value: Scenario): Either[PolicyErrors[CustomPolicyFailure], Vector[FeeDirective]] =
      Left(PolicyErrors.one(CustomPolicyRejected))
  val denomination = feePolicy
    .denomination(quote)(quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get
  val fee = feePolicy
    .percentage(
      denomination,
      FeeKind.from("negative-api").toOption.get,
      trading.quantity.refinement.NonNegative(Quantity(quote.dimension.ref, Rational(10))).toOption.get,
      FeeRate(Rational(1, 1000))
    )
    .toOption
    .get
  val directive = FeeDirective(fee, SliceIndex.zero)

  // OFFENDING-BEGIN
  object RemovedSchedule:
    val value: Class[trading.fee.policy.FeeSchedule[?, ?, ?, ?]] =
      classOf[trading.fee.policy.FeeSchedule[?, ?, ?, ?]]

  object UnconditionalMonoid:
    val value = summon[Monoid[Policy[Nothing]]]

  object EffectParameter:
    val value: FeePolicy[[A] =>> Either[String, A], D, B, Q, S] = noFees

  object ThrowableErasure:
    val value: Either[PolicyErrors[Throwable], Vector[FeeDirective]] = failing.evaluate(scenario)

  object StringErasure:
    val value: Either[PolicyErrors[String], Vector[FeeDirective]] = failing.evaluate(scenario)

  object ArbitraryMarket:
    val value = directive.sourceMarket
  // OFFENDING-END
end UnlawfulPolicyApi
