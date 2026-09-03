package trading.execution;

public final class RejectedExecutionIdentityConstruction {
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ExecutionLifecycle forgeLifecycle() {
    return new ExecutionLifecycle(null, null, null, null, null, null);
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
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static LifecycleReplayResult forgeLifecycleReplay() {
    return new LifecycleReplayResult(null, null);
  }
}
