package external.execution.positive;

import scala.Option;
import trading.execution.ApplicationCommandId;
import trading.execution.DispatchEvidence;
import trading.execution.ExecutionCommand;
import trading.execution.ExecutionLifecycle;
import trading.execution.ExecutionOrderId;
import trading.execution.ExecutionState;
import trading.execution.LifecycleTransition;
import trading.execution.QualifiedSourceEventId;
import trading.execution.QualifiedSourceOrderId;
import trading.execution.SourceEvidenceState;
import trading.execution.SourceFact;
import trading.execution.SourceFactTransition;
import trading.execution.SourceOrdering;
import trading.execution.SubmitOrderCommand;
import trading.execution.TransitionWork;
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

  private static final class ForgedSourceFact extends SourceFact {
    private ForgedSourceFact() {
      super();
    }

    @Override
    public QualifiedSourceEventId eventId() {
      return null;
    }

    @Override
    public ExecutionOrderId executionOrderId() {
      return null;
    }

    @Override
    public QualifiedSourceOrderId sourceOrderId() {
      return null;
    }

    @Override
    public SourceOrdering ordering() {
      return null;
    }
  }

  private static final class ForgedSourceTransition extends SourceFactTransition {
    private ForgedSourceTransition() {
      super();
    }

    @Override
    public SourceEvidenceState state() {
      return null;
    }
  }

  private static final class ForgedLifecycleTransition extends LifecycleTransition {
    private ForgedLifecycleTransition() {
      super();
    }

    @Override
    public ExecutionState state() {
      return null;
    }

    @Override
    public TransitionWork work() {
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
      // expected
    }
    try {
      new ForgedSourceFact();
      return false;
    } catch (IllegalAccessError expected) {
      // expected
    }
    try {
      new ForgedSourceTransition();
      return false;
    } catch (IllegalAccessError expected) {
      // expected
    }
    try {
      new ForgedLifecycleTransition();
      return false;
    } catch (IllegalAccessError expected) {
      return true;
    }
  }
}
