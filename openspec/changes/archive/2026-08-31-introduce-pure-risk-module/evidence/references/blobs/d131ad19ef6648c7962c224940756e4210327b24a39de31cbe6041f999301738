package external.risk.negative

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.risk.Risk

object InvalidRiskInputs:
  def acceptsBudget[D <: Dim](budget: NonNegative[Quantity[D]]): NonNegative[Quantity[D]] = budget

  // OFFENDING-BEGIN
  def rawBudget[D <: Dim](budget: Quantity[D]): NonNegative[Quantity[D]] =
    acceptsBudget(budget)

  def wrongSettlement[D <: Dim](instrument: Instrument, pnl: Pnl[D]) =
    Risk.downside(instrument)(pnl)
  // OFFENDING-END
end InvalidRiskInputs
