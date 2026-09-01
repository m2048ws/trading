package external.risk.negative

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.risk.*

object InvalidRiskInputs:
  def acceptsBudget[D <: Dim](budget: NonNegative[Quantity[D]]): NonNegative[Quantity[D]] = budget

  // OFFENDING-BEGIN
  def rawBudget[D <: Dim](budget: Quantity[D]): NonNegative[Quantity[D]] =
    acceptsBudget(budget)

  def wrongSettlement[D <: Dim](instrument: Instrument, pnl: Pnl[D]) =
    Risk.downside(instrument)(pnl)

  def rawAffineMarginal(
    instrument: Instrument
  )(
    cap: PositiveWhole,
    first: Quantity[instrument.roles.settle.D],
    marginal: Quantity[instrument.roles.settle.D]
  ) =
    MonotoneLotRisk.affine(instrument)(cap, first, marginal)

  def arbitraryPromise[D <: Dim, S <: Dim](evaluate: PositiveWhole => Quantity[S]) =
    MonotoneLotRisk.fromFunction(evaluate)
  // OFFENDING-END
end InvalidRiskInputs
