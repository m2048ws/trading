package trading.reference;

import java.io.ObjectStreamException;
import java.util.Objects;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import trading.quantity.JavaSerializationUnsupported;

/** Stable external identifier for an asset. */
public final class AssetId implements JavaSerializationUnsupported {
  private final String value;

  private AssetId(String value) {
    this.value = value;
  }

  /** Validate an external asset identifier. Null is rejected before a result is returned. */
  public static Either<EmptyAssetId$, AssetId> from(String value) {
    String nonNull = Objects.requireNonNull(value, "asset ID");
    return nonNull.trim().isEmpty()
        ? new Left<>(EmptyAssetId$.MODULE$)
        : new Right<>(new AssetId(nonNull));
  }

  public String value() {
    return value;
  }

  @Override
  public Object writeReplace() throws ObjectStreamException {
    return JavaSerializationUnsupported.super.writeReplace();
  }

  @Override
  public Object readResolve() throws ObjectStreamException {
    return JavaSerializationUnsupported.super.readResolve();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof AssetId that && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return "AssetId(" + value + ")";
  }
}
