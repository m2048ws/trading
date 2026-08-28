package trading.reference;

import java.io.ObjectStreamException;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import trading.quantity.JavaSerializationUnsupported;

/** Positive version distinguishing immutable definitions that share a {@link GridId}. */
public final class GridVersion implements JavaSerializationUnsupported {
  private final long value;

  private GridVersion(long value) {
    this.value = value;
  }

  /** Validate a positive stable grid version. */
  public static Either<NonPositiveGridVersion, GridVersion> from(long value) {
    return value > 0
        ? new Right<>(new GridVersion(value))
        : new Left<>(new NonPositiveGridVersion(value));
  }

  public long value() {
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
    return other instanceof GridVersion that && value == that.value;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(value);
  }

  @Override
  public String toString() {
    return "GridVersion(" + value + ")";
  }
}
