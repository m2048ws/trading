package external.risk.negative;

import scala.collection.immutable.Vector;
import trading.risk.AffordableUpperBoundary;
import trading.risk.MaxAffordableLots;

public final class RejectedRiskDecisionImplementations {
  private interface Attempt {
    Object construct();
  }

  private static boolean rejected(Attempt attempt) {
    try {
      attempt.construct();
      return false;
    } catch (IllegalAccessError expected) {
      return true;
    }
  }

  public static boolean guardsRejectUnknownAlternatives() {
    return rejected(ForgedDecision::new) && rejected(ForgedBoundary::new);
  }

  private static final class ForgedDecision extends MaxAffordableLots {
    @Override public Vector observations() { return null; }
  }

  private static final class ForgedBoundary extends AffordableUpperBoundary {}
}
