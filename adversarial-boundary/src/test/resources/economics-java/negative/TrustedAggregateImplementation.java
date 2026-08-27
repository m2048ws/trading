package external.economics.java.negative;

import trading.economics.InstrumentOrder;

// OFFENDING-BEGIN
abstract class TrustedAggregateImplementation
    extends InstrumentOrder<Object, String, String> {}
// OFFENDING-END
