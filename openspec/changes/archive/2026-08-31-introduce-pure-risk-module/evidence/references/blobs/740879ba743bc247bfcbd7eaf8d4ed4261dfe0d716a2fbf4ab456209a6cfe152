package external.risk.positive

import trading.economics.instrument.InstrumentId
import trading.economics.instrument.Instrument
import trading.quantity.*
import trading.quantity.refinement.*
import trading.risk.*

object RiskBoundaryClient:
  private def required[A](value: Either[?, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)

  val expected = required(InstrumentId.from("risk-boundary-expected"))
  val supplied = required(InstrumentId.from("risk-boundary-supplied"))
  val mismatch: RiskIdentityError = DownsideInstrumentMismatch(expected, supplied)

  def downside(instrument: Instrument)(pnl: instrument.Pnl) =
    Risk.downside(instrument)(pnl)

  def acceptsBudget[D <: Dim](budget: NonNegative[Quantity[D]]): NonNegative[Quantity[D]] = budget

  def run(): Unit =
    assert(mismatch == DownsideInstrumentMismatch(expected, supplied))
end RiskBoundaryClient
