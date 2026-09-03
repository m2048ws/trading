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
  val absent = new SourceOrderAbsent[Dim, Dim, Dim](null, null, null, null, null)
  val classifications = new SourceFactClassifications(Vector.empty)
  val sourceConflict = new SourceFactConflict[Dim, Dim, Dim](null, null)
  val fillConflict = new FillIdentityConflict[Dim, Dim, Dim](null, null)
  val positionConflict = new StreamPositionConflict[Dim, Dim, Dim](null, Vector.empty)
  val unresolved = new UnresolvedFillReference[Dim, Dim, Dim](null, null)
  val sourceRecorded = new SourceFactRecorded[Dim, Dim, Dim](null, null)
  val sourceRejected = new SourceFactRejected[Dim, Dim, Dim](null, null)
  val sourceState = new SourceEvidenceState[Dim, Dim, Dim](
    null, Map.empty, Map.empty, Vector.empty, Vector.empty, Map.empty, Map.empty, Map.empty)
  val submissionConflicts = new SubmissionConflicts(Vector.empty)
  val submissionEvidence = new SubmissionEvidence[Dim, Dim, Dim](
    Set.empty, Set.empty, Set.empty, Set.empty, Set.empty, Set.empty)
  val pendingSubmission = new IssuedPendingSubmission[Dim, Dim, Dim](null)
  val acceptedSubmission = new AcceptedSubmission[Dim, Dim, Dim](null)
  val rejectedSubmission = new RejectedSubmission[Dim, Dim, Dim](null)
  val notDispatchedSubmission = new ProvenNotDispatchedSubmission[Dim, Dim, Dim](null)
  val indeterminateSubmission = new IndeterminateSubmission[Dim, Dim, Dim](null)
  val executionProvenSubmission = new ExecutionProvenSubmission[Dim, Dim, Dim](null)
  val absentSubmission = new AuthoritativelyAbsentSubmission[Dim, Dim, Dim](null)
  val conflictingSubmission = new ConflictingSubmission[Dim, Dim, Dim](null, null)
  val modifierAmbiguity = new ModifierAmbiguity(Vector.empty)
  val activeEffectiveFill = new ActiveEffectiveFill[Dim, Dim, Dim](null, null, null, Vector.empty)
  val bustedEffectiveFill = new BustedEffectiveFill[Dim, Dim, Dim](null, null, Vector.empty)
  val ambiguousEffectiveFill = new AmbiguousEffectiveFill[Dim, Dim, Dim](null, Vector.empty, null)
  val conflictingEffectiveFill = new ConflictingEffectiveFill[Dim, Dim, Dim](
    null, Vector.empty, Vector.empty, Vector.empty)
  val effectiveFillLedger = new EffectiveFillLedger[Dim, Dim, Dim](null, null, None, null)
  val cancellationEvidence = new CancellationEvidence[Dim, Dim, Dim](
    Set.empty, Set.empty, Set.empty, Set.empty, Set.empty, Set.empty, Set.empty)
  val cancellationRequested = new CancellationRequested[Dim, Dim, Dim](null)
  val cancellationConfirmed = new CancellationConfirmed[Dim, Dim, Dim](null)
  val cancellationConflicted = new CancellationConflicted[Dim, Dim, Dim](null)
  val postCancellationFill = new PostCancellationFillAnomaly[Dim, Dim, Dim](null, Vector.empty, null)
  val executionAnomalies = new ExecutionAnomalies[Dim, Dim, Dim](None, Vector.empty, Set.empty, Set.empty, Map.empty)
  val executionState = new ExecutionState[Dim, Dim, Dim](null, null, null, null)
  val lifecycleAccepted = new LifecycleAccepted[Dim, Dim, Dim](null, null, null)
  val lifecycleRejected = new LifecycleRejected[Dim, Dim, Dim](null, null, null)
  val lifecycleDiagnostics = new LifecycleDiagnostics(Vector.empty)
  val lifecycleObservation = new LifecycleObservation[Dim, Dim, Dim](
    null, None, None, Map.empty, Map.empty, Map.empty, null, null, Vector.empty, Vector.empty, Vector.empty, Map.empty,
    Map.empty, Set.empty, Vector.empty, Map.empty, None)
  val lifecycleReplay = new LifecycleReplayResult[Dim, Dim, Dim](null, Vector.empty)
  val copied = checkedTarget.copy(source = null)
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
  final class ForgedSubmissionKnowledge extends SubmissionKnowledge[Dim, Dim, Dim]():
    val evidence = null
  final class ForgedEffectiveFill extends EffectiveFill[Dim, Dim, Dim]():
    val original = null
    val modifiers = Vector.empty
  final class ForgedCancellationKnowledge extends CancellationKnowledge[Dim, Dim, Dim]():
    val evidence = null
  val nativeAmend = null.asInstanceOf[SubmitOrderCommand[Dim, Dim, Dim]].amend(null)
  val atomicReplace = null.asInstanceOf[CommandState[Dim, Dim, Dim]].cancelReplace(null)
  // OFFENDING-END
