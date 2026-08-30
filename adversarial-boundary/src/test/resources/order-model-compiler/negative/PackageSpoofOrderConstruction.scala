package trading.order

import trading.quantity.Dim

object PackageSpoofOrderConstruction:
  def forge[D <: Dim, B <: Dim, Q <: Dim](
    intent: OrderIntent[D],
    checked: CheckedActivation[B, Q],
    fixed: FixedTriggerEvidence[B, Q],
    trailing: TrailingTriggerEvidence[B, Q],
    peg: PegResolution[B, Q]
  ): Unit =
    // OFFENDING-BEGIN
    val rawChecked    = new CheckedActivation[B, Q](checked.observations)
    val copiedChecked = checked.copy(observations = checked.observations)
    val rawFixed = new FixedTriggerEvidence[B, Q](
      fixed.reference,
      fixed.comparison,
      fixed.triggerPrice,
      fixed.observedPrice
    )
    val rawTrailing = new TrailingTriggerEvidence[B, Q](
      trailing.reference,
      trailing.comparison,
      trailing.offsetTicks,
      trailing.favorableExtreme,
      trailing.observedPrice
    )
    val rawPeg = new PegResolution[B, Q](
      peg.reference,
      peg.offsetTicks,
      peg.referencePrice,
      peg.resolvedLimit
    )
    val rawIntent = new OrderIntent[D](
      intent.instrumentId,
      intent.side,
      intent.lots,
      intent.positionEffect,
      intent.positionChange
    )
    val copiedIntent = intent.copy(positionChange = intent.positionChange)
    // OFFENDING-END
    val _ = intent
end PackageSpoofOrderConstruction
