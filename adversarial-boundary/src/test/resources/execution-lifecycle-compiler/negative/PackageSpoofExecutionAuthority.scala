package trading.execution

import trading.quantity.Dim

object PackageSpoofExecutionAuthority:
  val baseline = "checked factories only"
  val checkedTarget = ExecutionTarget
    .create(
      ExecutionSourceId.from("source").toOption.get,
      ExecutionAccountId.from("account").toOption.get
    )
    .toOption
    .get

  // OFFENDING-BEGIN
  val target = new ExecutionTarget(null, null)
  val event = new QualifiedSourceEventId(null, null)
  val stream = new QualifiedSourceStreamId(null, null)
  val position = new QualifiedStreamPosition(null, null)
  val sequenced = new AuthoritativelySequenced(null, null)
  val checkpoint = new SourceCheckpoint(null, null)
  val completeness = new SourceCompleteness(null)
  val lifecycle = new ExecutionLifecycle[Dim, Dim, Dim](null, null, null, null, null, null)
  val commandErrors = new CommandViolations(Vector.empty)
  val submit = new SubmitOrderCommand[Dim, Dim, Dim](null, null)
  val cancel = new CancelOrderCommand[Dim, Dim, Dim](null, null, null)
  val notDispatched = new ProvenNotDispatched[Dim, Dim, Dim](null)
  val indeterminate = new IndeterminateDispatch[Dim, Dim, Dim](null)
  val conflict = new CommandConflict[Dim, Dim, Dim](null, null)
  val commandState = new CommandState[Dim, Dim, Dim](null, Map.empty, Map.empty, Vector.empty, Vector.empty)
  val transition = new CommandTransition[Dim, Dim, Dim](null, null, None)
  val sourceErrors = new SourceFactViolations(Vector.empty)
  val accepted = new OrderAccepted[Dim, Dim, Dim](null, null, null, null)
  val rejected = new OrderRejected[Dim, Dim, Dim](null, null, null, null)
  val executionFill = new ExecutionFill[Dim, Dim, Dim](null, null, null, null, null, null, null)
  val corrected = new FillCorrected[Dim, Dim, Dim](null, null, null, null, null, null, null)
  val busted = new FillBusted[Dim, Dim, Dim](null, null, null, null, null)
  val effectiveCancellation = new CancellationEffective[Dim, Dim, Dim](null, null, null, null)
  val reconciliation = new ReconciliationCheckpoint[Dim, Dim, Dim](null, null, null, null, null)
  val completed = new SourceOrderCompleted[Dim, Dim, Dim](null, null, null, null, null)
  val classifications = new SourceFactClassifications(Vector.empty)
  val sourceConflict = new SourceFactConflict[Dim, Dim, Dim](null, null)
  val fillConflict = new FillIdentityConflict[Dim, Dim, Dim](null, null)
  val positionConflict = new StreamPositionConflict[Dim, Dim, Dim](null, Vector.empty)
  val unresolved = new UnresolvedFillReference[Dim, Dim, Dim](null, null)
  val sourceRecorded = new SourceFactRecorded[Dim, Dim, Dim](null, null)
  val sourceRejected = new SourceFactRejected[Dim, Dim, Dim](null, null)
  val sourceState = new SourceEvidenceState[Dim, Dim, Dim](
    null, Map.empty, Map.empty, Vector.empty, Vector.empty, Map.empty, Map.empty, Map.empty)
  val executionState = new ExecutionState[Dim, Dim, Dim](null, null, null, null)
  val lifecycleAccepted = new LifecycleAccepted[Dim, Dim, Dim](null, null, null)
  val lifecycleRejected = new LifecycleRejected[Dim, Dim, Dim](null, null, null)
  val lifecycleDiagnostics = new LifecycleDiagnostics(Vector.empty)
  val lifecycleObservation = new LifecycleObservation[Dim, Dim, Dim](
    null, Map.empty, Map.empty, Map.empty, Vector.empty, Vector.empty, Vector.empty, Map.empty, Map.empty, Set.empty,
    Vector.empty, Map.empty, None)
  val lifecycleReplay = new LifecycleReplayResult[Dim, Dim, Dim](null, Vector.empty)
  val copied = checkedTarget.copy(source = null)
  final class ForgedContinuation extends SourceContinuation()
  final class ForgedOrdering extends SourceOrdering()
  final class ForgedCommand extends ExecutionCommand[Dim, Dim, Dim]():
    val commandId = null
    val lifecycle = null
  final class ForgedDispatch extends DispatchEvidence[Dim, Dim, Dim]():
    val submit = null
  final class ForgedSourceFact extends SourceFact[Dim, Dim, Dim]():
    val eventId = null
    val executionOrderId = null
    val sourceOrderId = null
    val ordering = null
  final class ForgedSourceTransition extends SourceFactTransition[Dim, Dim, Dim]():
    val state = null
  final class ForgedLifecycleTransition extends LifecycleTransition[Dim, Dim, Dim]():
    val state = null
    val work = null
  val nativeAmend = null.asInstanceOf[SubmitOrderCommand[Dim, Dim, Dim]].amend(null)
  val atomicReplace = null.asInstanceOf[CommandState[Dim, Dim, Dim]].cancelReplace(null)
  // OFFENDING-END
