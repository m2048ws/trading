package external.risk.positive

import trading.economics.instrument.InstrumentId
import trading.risk.*

object RiskBoundaryClient:
  private def required[A](value: Either[?, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)

  val expected = required(InstrumentId.from("risk-boundary-expected"))
  val supplied = required(InstrumentId.from("risk-boundary-supplied"))
  val mismatch: RiskIdentityError = PnlInstrumentMismatch(expected, supplied)

  def run(): Unit =
    assert(mismatch == PnlInstrumentMismatch(expected, supplied))
end RiskBoundaryClient
