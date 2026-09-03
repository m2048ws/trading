package external.codec.positive;

import scala.util.Either;
import trading.codec.DecodeLimits;
import trading.codec.RecordType;
import trading.codec.SchemaVersion;
import trading.codec.WireViolations;

public final class BoundaryCodecJavaClient {
  private BoundaryCodecJavaClient() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static boolean checkedFactoriesPreserveSemantics() {
    Either recordType = RecordType.from("trading.java-client");
    Either version = SchemaVersion.from(scala.math.BigInt.apply(2));
    Either limits = DecodeLimits.create(100, 400, 8, 10, 16, 20, 40, 20, 10, 10, 10, 10);
    WireViolations errors = WireViolations.one("first");

    return recordType.isRight()
        && version.isRight()
        && limits.isRight()
        && ((RecordType) recordType.toOption().get()).value().equals("trading.java-client")
        && ((SchemaVersion) version.toOption().get()).value().equals(scala.math.BigInt.apply(2))
        && errors.toVector().size() == 1;
  }
}
