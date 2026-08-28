package external.referencejava;

import scala.util.Either;
import scala.util.Right;
import trading.quantity.AtomId;
import trading.reference.AssetDefinition;
import trading.reference.AssetId;
import trading.reference.GridId;
import trading.reference.GridVersion;
import trading.reference.ReferenceDataError;

public abstract class SharedReferenceDataJavaSetup {
  @SuppressWarnings("unchecked")
  protected static <A> A right(Either<? extends ReferenceDataError, A> value) {
    if (value instanceof Right<?, ?> right) {
      return (A) right.value();
    }
    throw new AssertionError("expected Right, got " + value);
  }

  protected static AssetId assetId(String value) {
    return right(AssetId.from(value));
  }

  protected static GridId gridId(String value) {
    return right(GridId.from(value));
  }

  protected static GridVersion gridVersion(long value) {
    return right(GridVersion.from(value));
  }

  protected static AssetDefinition assetDefinition(String value) {
    return new AssetDefinition(assetId(value), new AtomId("java:" + value));
  }
}
