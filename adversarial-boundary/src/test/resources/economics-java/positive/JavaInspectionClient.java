package external.economics.java.positive;

import scala.math.BigInt;
import trading.economics.InstrumentLots;
import trading.economics.InstrumentPrice;
import trading.quantity.Dimension;
import trading.quantity.Rational;

public final class JavaInspectionClient {
  private JavaInspectionClient() {}

  public static BigInt count(InstrumentLots<?, ? extends Dimension> lots) {
    return lots.count();
  }

  public static Rational quantity(InstrumentLots<?, ? extends Dimension> lots) {
    return lots.quantity();
  }

  public static BigInt ticks(
      InstrumentPrice<?, ? extends Dimension, ? extends Dimension> price) {
    return price.ticks();
  }
}
