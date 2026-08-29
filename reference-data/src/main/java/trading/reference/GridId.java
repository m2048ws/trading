package trading.reference;

import java.io.ObjectStreamException;
import java.util.Objects;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import trading.quantity.JavaSerializationUnsupported;

/** Stable external identifier for a grid definition. */
public final class GridId implements JavaSerializationUnsupported {
  private final String value;

  private GridId(String value) {
    this.value = value;
  }

  /** Validate an external grid identifier. Null is rejected before a result is returned. */
  public static Either<EmptyGridId$, GridId> from(String value) {
    String nonNull = Objects.requireNonNull(value, "grid ID");
    return nonNull.trim().isEmpty()
        ? new Left<>(EmptyGridId$.MODULE$)
        : new Right<>(new GridId(nonNull));
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
    return other instanceof GridId that && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return "GridId(" + value + ")";
  }
}
