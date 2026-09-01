package trading.risk

import trading.economics.instrument.InstrumentId

/** Expected identity failures while measuring an instrument-owned PnL. */
sealed abstract class RiskIdentityError extends Product with Serializable

/** The supplied PnL belongs to a different instrument from the requested risk measurement. */
final case class PnlInstrumentMismatch(expected: InstrumentId, supplied: InstrumentId) extends RiskIdentityError
