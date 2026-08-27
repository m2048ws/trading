package external.economics.java.negative;

import scala.math.BigInt;
import trading.economics.InstrumentLots;
import trading.quantity.Dimension;
import trading.quantity.Rational;

// OFFENDING-BEGIN
final class TrustedCarrierImplementation extends InstrumentLots<Object, Dimension> {
  @Override
  public BigInt count() {
    throw new UnsupportedOperationException("invalid observation");
  }

  @Override
  public Rational quantity() {
    throw new UnsupportedOperationException("invalid observation");
  }
}
// OFFENDING-END
