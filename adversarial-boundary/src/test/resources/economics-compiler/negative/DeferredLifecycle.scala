package external.economics.negative

import trading.economics.instrument.Instrument

object DeferredLifecycle:
  def rejectDeferredState(
    instrument: Instrument,
    order: instrument.Order,
    scenario: instrument.OrderScenario,
    fee: instrument.Fee,
    pnl: instrument.Pnl
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
