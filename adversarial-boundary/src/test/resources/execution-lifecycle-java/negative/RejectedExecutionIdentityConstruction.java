package trading.execution;

public final class RejectedExecutionIdentityConstruction {
  public static ApplicationCommandId forgeCommand() {
    return new ApplicationCommandId("forged");
  }

  public static SourceSequence forgeSequence() {
    return new SourceSequence(scala.math.BigInt.apply(1));
  }

  public static ExecutionTarget forgeTarget() {
    return new ExecutionTarget(null, null);
  }

  public static QualifiedSourceEventId forgeEvent() {
    return new QualifiedSourceEventId(null, null);
  }

  public static AuthoritativelySequenced forgeOrdering() {
    return new AuthoritativelySequenced(null, null);
  }

  public static SourceCheckpoint forgeCheckpoint() {
    return new SourceCheckpoint(null, null);
  }

  public static SourceCompleteness forgeCompleteness() {
    return new SourceCompleteness(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ExecutionLifecycle forgeLifecycle() {
    return new ExecutionLifecycle(null, null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static SubmitOrderCommand forgeSubmit() {
    return new SubmitOrderCommand(null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static CancelOrderCommand forgeCancel() {
    return new CancelOrderCommand(null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ProvenNotDispatched forgeProvenNotDispatched() {
    return new ProvenNotDispatched(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static IndeterminateDispatch forgeIndeterminateDispatch() {
    return new IndeterminateDispatch(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static CommandState forgeCommandState() {
    return new CommandState(null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ExecutionFill forgeFill() {
    return new ExecutionFill(null, null, null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static FillCorrected forgeCorrection() {
    return new FillCorrected(null, null, null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static FillBusted forgeBust() {
    return new FillBusted(null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static SourceOrderAbsent forgeSourceOrderAbsent() {
    return new SourceOrderAbsent(null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static SourceEvidenceState forgeSourceState() {
    return new SourceEvidenceState(null, null, null, null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ExecutionState forgeExecutionState() {
    return new ExecutionState(null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static SubmissionEvidence forgeSubmissionEvidence() {
    return new SubmissionEvidence(null, null, null, null, null, null);
  }

  public static SubmissionConflicts forgeSubmissionConflicts() {
    return new SubmissionConflicts(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static IssuedPendingSubmission forgePendingSubmission() {
    return new IssuedPendingSubmission(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static AcceptedSubmission forgeAcceptedSubmission() {
    return new AcceptedSubmission(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static RejectedSubmission forgeRejectedSubmission() {
    return new RejectedSubmission(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ProvenNotDispatchedSubmission forgeNotDispatchedSubmission() {
    return new ProvenNotDispatchedSubmission(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static IndeterminateSubmission forgeIndeterminateSubmission() {
    return new IndeterminateSubmission(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ExecutionProvenSubmission forgeExecutionProvenSubmission() {
    return new ExecutionProvenSubmission(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static AuthoritativelyAbsentSubmission forgeAbsentSubmission() {
    return new AuthoritativelyAbsentSubmission(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ConflictingSubmission forgeConflictingSubmission() {
    return new ConflictingSubmission(null, null);
  }

  public static ModifierAmbiguity forgeModifierAmbiguity() {
    return new ModifierAmbiguity(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ActiveEffectiveFill forgeActiveEffectiveFill() {
    return new ActiveEffectiveFill(null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static BustedEffectiveFill forgeBustedEffectiveFill() {
    return new BustedEffectiveFill(null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static AmbiguousEffectiveFill forgeAmbiguousEffectiveFill() {
    return new AmbiguousEffectiveFill(null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ConflictingEffectiveFill forgeConflictingEffectiveFill() {
    return new ConflictingEffectiveFill(null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static EffectiveFillLedger forgeEffectiveFillLedger() {
    return new EffectiveFillLedger(null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static LifecycleAccepted forgeLifecycleAccepted() {
    return new LifecycleAccepted(null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static LifecycleRejected forgeLifecycleRejected() {
    return new LifecycleRejected(null, null, null);
  }

  public static LifecycleDiagnostics forgeLifecycleDiagnostics() {
    return new LifecycleDiagnostics(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static LifecycleObservation forgeLifecycleObservation() {
    return new LifecycleObservation(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static LifecycleReplayResult forgeLifecycleReplay() {
    return new LifecycleReplayResult(null, null);
  }
}
