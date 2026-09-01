package external.execution.positive;

import scala.Option;
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
      return true;
    }
  }
}
