package external.order.positive;

import scala.util.Either;
import trading.order.DirectPricingResolution$;
import trading.order.ImmediateActivation;
import trading.order.MarketExecution;
import trading.order.NonRestingTimeInForce;
import trading.order.TimeInForce;

public final class OrderFactoryClient {
  private OrderFactoryClient() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static boolean checkedFactoriesPreserveSemantics() {
    ImmediateActivation immediate = ImmediateActivation.apply();
    Either activationCheck = immediate.verify(immediate.evidence());

    Either ioc = NonRestingTimeInForce.from(TimeInForce.valueOf("ImmediateOrCancel"));
    Either resting = NonRestingTimeInForce.from(TimeInForce.valueOf("Day"));
    NonRestingTimeInForce duration = (NonRestingTimeInForce) ioc.toOption().get();
    MarketExecution market = MarketExecution.apply(duration);

    return activationCheck.isRight()
        && ioc.isRight()
        && resting.isLeft()
        && market.resolve(DirectPricingResolution$.MODULE$).isRight();
  }
}
