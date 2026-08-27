package trading.economics;

import scala.util.Either;

/** JVM construction gate for instrument-owned values. */
final class JvmOwnerAuthority {
  private JvmOwnerAuthority() {}

  static Either<EconomicsError, Instrument> createInstrument(InstrumentDefinition definition) {
    return Instrument.createWithAuthority(definition, new JvmOwnerAuthority());
  }

  void assertIssued() {}
}
