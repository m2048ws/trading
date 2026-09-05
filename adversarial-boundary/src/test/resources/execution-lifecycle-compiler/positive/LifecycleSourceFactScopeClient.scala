package external.execution.positive

import external.execution.fixtures.ExecutionLifecycleSetup.*
import trading.execution.*

object LifecycleSourceFactScopeClient:
  private type PositionD = instrument.PositionD
  private type BaseD     = instrument.BaseD
  private type QuoteD    = instrument.QuoteD

  private def required[A](value: Either[?, A]): A =
    value.fold(error => throw new AssertionError(error.toString), identity)

  def run(): Unit =
    val target = required(
      ExecutionTarget.create(
        required(ExecutionSourceId.from("source")),
        required(ExecutionAccountId.from("account"))
      )
    )
    val lifecycle = required(
      ExecutionLifecycle.create(instrument)(
        order,
        required(ExecutionOrderId.from("logical-order")),
        required(OrderLineageId.from("lineage")),
        target
      )
    )
    val sourceOrderId = required(
      QualifiedSourceOrderId.create(target, required(NativeSourceOrderId.from("source-order")))
    )
    val fillId = required(QualifiedFillId.create(target, required(NativeFillId.from("fill"))))
    val stream = required(QualifiedSourceStreamId.create(target, required(SourceStreamId.from("orders"))))
    val position = required(QualifiedStreamPosition.create(stream, required(SourceSequence.from(BigInt(2)))))
    val checkpoint = required(SourceCheckpoint.create(position, required(SourceContinuation.origin(stream))))
    val completeness = required(SourceCompleteness.create(position))
    val scope        = SourceFact.forLifecycle(lifecycle)

    def event(value: String): QualifiedSourceEventId =
      required(QualifiedSourceEventId.create(target, required(NativeSourceEventId.from(value))))

    val accepted: Either[SourceFactViolations, OrderAccepted[PositionD, BaseD, QuoteD]] =
      scope.accepted(event("accepted"), lifecycle.executionOrderId, sourceOrderId, SourceOrdering.unsequenced)
    val rejected: Either[SourceFactViolations, OrderRejected[PositionD, BaseD, QuoteD]] =
      scope.rejected(event("rejected"), lifecycle.executionOrderId, sourceOrderId, SourceOrdering.unsequenced)
    val fill: Either[SourceFactViolations, ExecutionFill[PositionD, BaseD, QuoteD]] =
      scope.fill(
        event("fill"),
        lifecycle.executionOrderId,
        sourceOrderId,
        fillId,
        lots,
        price,
        SourceOrdering.unsequenced
      )
    val corrected: Either[SourceFactViolations, FillCorrected[PositionD, BaseD, QuoteD]] =
      scope.corrected(
        event("corrected"),
        lifecycle.executionOrderId,
        sourceOrderId,
        fillId,
        lots,
        price,
        SourceOrdering.unsequenced
      )
    val busted: Either[SourceFactViolations, FillBusted[PositionD, BaseD, QuoteD]] =
      scope.busted(event("busted"), lifecycle.executionOrderId, sourceOrderId, fillId, SourceOrdering.unsequenced)
    val cancelled: Either[SourceFactViolations, CancellationEffective[PositionD, BaseD, QuoteD]] =
      scope.cancellationEffective(
        event("cancelled"),
        lifecycle.executionOrderId,
        sourceOrderId,
        SourceOrdering.unsequenced
      )
    val reconciled: Either[SourceFactViolations, ReconciliationCheckpoint[PositionD, BaseD, QuoteD]] =
      scope.reconciliationCheckpoint(
        event("checkpoint"),
        lifecycle.executionOrderId,
        sourceOrderId,
        checkpoint,
        SourceOrdering.unsequenced
      )
    val completed: Either[SourceFactViolations, SourceOrderCompleted[PositionD, BaseD, QuoteD]] =
      scope.sourceOrderCompleted(
        event("completed"),
        lifecycle.executionOrderId,
        sourceOrderId,
        completeness,
        SourceOrdering.unsequenced
      )
    val absent: Either[SourceFactViolations, SourceOrderAbsent[PositionD, BaseD, QuoteD]] =
      scope.sourceOrderAbsent(
        event("absent"),
        lifecycle.executionOrderId,
        sourceOrderId,
        completeness,
        SourceOrdering.unsequenced
      )

    assert(
      Vector(accepted, rejected, fill, corrected, busted, cancelled, reconciled, completed, absent).forall(_.isRight)
    )
end LifecycleSourceFactScopeClient
