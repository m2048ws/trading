package external.economics.java.negative;

import trading.economics.Instrument;

final class ExternalAuthorityAccess {
  // OFFENDING-BEGIN
  static Instrument.OwnerAuthority<Object> manufacture() {
    return new Instrument.OwnerAuthority<>();
  }
  // OFFENDING-END
}
