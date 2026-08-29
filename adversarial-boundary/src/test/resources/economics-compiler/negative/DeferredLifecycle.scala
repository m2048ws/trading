package external.economics.negative

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.Dim
import trading.scenario.*

object DeferredLifecycle:
  def rejectDeferredState[D <: Dim, S <: Dim, L, P, M, Pos](
    order: Order[L, P],
    scenario: OrderScenario[L, P, M, Pos],
    fee: Fee[D],
    pnl: Pnl[S]
  ): Unit =
    val _ = order.intent.side
    val _ = scenario.assumptions.matchedSlices
    val _ = fee.amount
    val _ = pnl.netPnl

    // OFFENDING-BEGIN
    val _ = order.venueOrderId
    val _ = order.submissionStatus
    val _ = scenario.fills
    val _ = fee.reportedFee
    val _ = pnl.funding
    val _ = pnl.margin
    val _ = pnl.liquidation
    val _ = pnl.ledger
    val _ = pnl.account
    // OFFENDING-END
end DeferredLifecycle
