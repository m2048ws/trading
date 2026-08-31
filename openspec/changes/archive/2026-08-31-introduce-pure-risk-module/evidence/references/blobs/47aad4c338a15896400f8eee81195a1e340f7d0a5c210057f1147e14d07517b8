package trading.risk

import trading.economics.instrument.InstrumentId

/** Expected identity failures while measuring an instrument-owned PnL. */
sealed abstract class RiskIdentityError extends Product with Serializable:
  def expected: InstrumentId
  def supplied: InstrumentId

/** The supplied PnL belongs to a different instrument from the requested risk measurement. */
final case class DownsideInstrumentMismatch(expected: InstrumentId, supplied: InstrumentId) extends RiskIdentityError

/** Closed input locations for checked lot-risk assessment construction. */
enum AssessmentInputLocation:
  case Lots, Pnl

/** One assessment input belongs to a different instrument. */
final case class AssessmentInstrumentMismatch(
  location: AssessmentInputLocation,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends RiskIdentityError
