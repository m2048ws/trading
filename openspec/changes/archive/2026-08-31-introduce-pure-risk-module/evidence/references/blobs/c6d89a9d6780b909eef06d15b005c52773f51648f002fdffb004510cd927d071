package trading.risk

import trading.economics.instrument.*
import trading.fee.policy.FeePolicyError
import trading.quantity.Rational
import trading.scenario.RoundTripViolation

sealed abstract class RiskError extends Product with Serializable
final case class RiskInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends RiskError
final case class InvalidRiskBudget(coefficient: Rational)                                extends RiskError
final case class RiskLotFailure(cause: LotError)                                         extends RiskError
final case class RiskScenarioFailure(cause: RoundTripViolation)                          extends RiskError
final case class RiskFeePolicyFailure(cause: FeePolicyError)                             extends RiskError
final case class SizingScenarioMismatch(candidateLots: BigInt, heldPositionLots: BigInt) extends RiskError
