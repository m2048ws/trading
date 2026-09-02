package external.execution.positive;

import scala.Option;
import trading.execution.ApplicationCommandId;
import trading.execution.DispatchEvidence;
import trading.execution.ExecutionCommand;
import trading.execution.ExecutionLifecycle;
import trading.execution.SubmitOrderCommand;
import trading.execution.QualifiedSourceStreamId;
import trading.execution.QualifiedStreamPosition;
import trading.execution.SourceContinuation;
import trading.execution.SourceOrdering;

public final class RejectedExecutionAlternatives {
  private static final class ForgedContinuation extends SourceContinuation {
    private ForgedContinuation() {
      super();
    }

    @Override
    public QualifiedSourceStreamId stream() {
      return null;
    }

    @Override
    public Option<QualifiedStreamPosition> previous() {
      return Option.empty();
    }
  }

  private static final class ForgedOrdering extends SourceOrdering {
    private ForgedOrdering() {
      super();
    }
  }

  private static final class ForgedCommand extends ExecutionCommand {
    private ForgedCommand() {
      super();
    }

    @Override
    public ApplicationCommandId commandId() {
      return null;
    }

    @Override
    public ExecutionLifecycle lifecycle() {
      return null;
    }
  }

  private static final class ForgedDispatch extends DispatchEvidence {
    private ForgedDispatch() {
      super();
    }

    @Override
    public SubmitOrderCommand submit() {
      return null;
    }
  }

  public static boolean guardsRejectUnknownAlternatives() {
    try {
      new ForgedContinuation();
      return false;
    } catch (IllegalAccessError expected) {
      // expected
    }
    try {
      new ForgedOrdering();
      return false;
    } catch (IllegalAccessError expected) {
      // expected
    }
    try {
      new ForgedCommand();
      return false;
    } catch (IllegalAccessError expected) {
      // expected
    }
    try {
      new ForgedDispatch();
      return false;
    } catch (IllegalAccessError expected) {
      return true;
    }
  }
}
