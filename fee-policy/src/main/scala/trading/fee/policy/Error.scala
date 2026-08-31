package trading.fee.policy

import trading.economics.instrument.*
import trading.fee.PolicyErrors
import trading.scenario.ScenarioLeg

sealed abstract class FeePolicyError extends Product with Serializable
final case class FeePolicyInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends FeePolicyError
final case class FeePolicyFailures(causes: PolicyErrors[FeePolicyError]) extends FeePolicyError
final case class InvalidFeeAttribution(sliceIndex: Int, sliceCount: Int) extends FeePolicyError
final case class ForeignScenarioLine(sourceSliceIndex: Int)              extends FeePolicyError
final case class FeeValueFailure(cause: FeeValueError)                   extends FeePolicyError
final case class FeeValuationFailure(cause: ValuationError)              extends FeePolicyError
final case class FeeContributionFailure(leg: ScenarioLeg, sliceIndex: Int, cause: ContributionError)
  extends FeePolicyError
final case class FeePnlFailure(cause: PnlError) extends FeePolicyError
