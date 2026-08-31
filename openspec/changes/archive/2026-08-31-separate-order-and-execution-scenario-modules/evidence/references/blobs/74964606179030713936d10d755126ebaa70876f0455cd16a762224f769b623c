package external.order.negative;

import scala.collection.immutable.Vector;
import scala.util.Either;
import trading.economics.instrument.InstrumentId;
import trading.order.ActivationViolation;
import trading.order.CheckedActivation;
import trading.order.EffectivePricing;
import trading.order.Order;
import trading.order.OrderActivation;
import trading.order.OrderExecution;
import trading.order.OrderIntent;
import trading.order.OrderPricing;
import trading.order.PricedVisibility;
import trading.order.PricingViolation;

public final class RejectedAlgebraImplementations {
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

  public static boolean guardsRejectEveryUnknownImplementation() {
    return rejected(ForgedOrder::new)
        && rejected(ForgedActivation::new)
        && rejected(ForgedExecution::new)
        && rejected(ForgedPricing::new)
        && rejected(ForgedVisibility::new);
  }

  private static final class ForgedOrder extends Order {
    @Override public InstrumentId instrumentId() { return null; }
    @Override public OrderIntent intent() { return null; }
    @Override public OrderActivation activation() { return null; }
    @Override public OrderExecution execution() { return null; }
  }

  private static final class ForgedActivation extends OrderActivation {
    @Override public Either<ActivationViolation, CheckedActivation> verify(Object evidence) { return null; }
    @Override public boolean acceptsEvidence(Object evidence) { return true; }
    @Override public Vector observations(Object evidence) { return null; }
  }

  private static final class ForgedExecution extends OrderExecution {
    @Override public Either<PricingViolation, EffectivePricing> resolve(Object resolution) { return null; }
    @Override public boolean acceptsResolution(Object resolution) { return true; }
    @Override public boolean requiresMaker() { return false; }
    @Override public Vector observations(Object resolution) { return null; }
  }

  private static final class ForgedPricing extends OrderPricing {
    @Override public Either<PricingViolation, EffectivePricing> resolve(Object resolution) { return null; }
    @Override public boolean acceptsResolution(Object resolution) { return true; }
    @Override public Vector observations(Object resolution) { return null; }
  }

  private static final class ForgedVisibility extends PricedVisibility {}
}
