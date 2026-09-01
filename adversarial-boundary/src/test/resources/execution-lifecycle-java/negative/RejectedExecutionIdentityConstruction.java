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
}
