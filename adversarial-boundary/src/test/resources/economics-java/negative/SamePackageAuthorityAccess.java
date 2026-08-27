package trading.economics;

final class SamePackageAuthorityAccess {
  // OFFENDING-BEGIN
  static Instrument.OwnerAuthority<Object> manufacture() {
    return new Instrument.OwnerAuthority<>(new JvmOwnerAuthority());
  }
  // OFFENDING-END
}
