package external.execution.negative

import trading.economics.instrument.*
import trading.execution.*
import trading.quantity.Dim

object LifecycleSourceFactScopeMismatch:
  def rejected[
    D <: Dim,
    B <: Dim,
    Q <: Dim,
    ForeignD <: Dim,
    ForeignB <: Dim,
    ForeignQ <: Dim
  ](
    scope: SourceFact.LifecycleScope[D, B, Q],
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    fillId: QualifiedFillId,
    lots: Lots[D],
    price: Price[B, Q],
    foreignLots: Lots[ForeignD],
    foreignPrice: Price[ForeignB, ForeignQ]
  ): Unit =
    val validFill = scope.fill(
      eventId,
      executionOrderId,
      sourceOrderId,
      fillId,
      lots,
      price,
      SourceOrdering.unsequenced
    )
    val validCorrection = scope.corrected(
      eventId,
      executionOrderId,
      sourceOrderId,
      fillId,
      lots,
      price,
      SourceOrdering.unsequenced
    )

    // OFFENDING-BEGIN
    val wrongFillLots = scope.fill(
      eventId,
      executionOrderId,
      sourceOrderId,
      fillId,
      foreignLots,
      price,
      SourceOrdering.unsequenced
    )
    val wrongFillPrice = scope.fill(
      eventId,
      executionOrderId,
      sourceOrderId,
      fillId,
      lots,
      foreignPrice,
      SourceOrdering.unsequenced
    )
    val wrongCorrectionLots = scope.corrected(
      eventId,
      executionOrderId,
      sourceOrderId,
      fillId,
      foreignLots,
      price,
      SourceOrdering.unsequenced
    )
    val wrongCorrectionPrice = scope.corrected(
      eventId,
      executionOrderId,
      sourceOrderId,
      fillId,
      lots,
      foreignPrice,
      SourceOrdering.unsequenced
    )
    // OFFENDING-END

    val _ = (validFill, validCorrection)
  end rejected
end LifecycleSourceFactScopeMismatch
