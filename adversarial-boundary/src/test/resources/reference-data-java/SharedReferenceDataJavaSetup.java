package external.referencejava;

import scala.util.Either;
import scala.util.Right;
import trading.quantity.AtomId;
import trading.reference.AssetDefinition;
import trading.reference.AssetId;
import trading.reference.CatalogBatch;
import trading.reference.CatalogCommand;
import trading.reference.CatalogCommand.RegisterAsset;
import trading.reference.CatalogModel;
import trading.reference.CatalogState;
import trading.reference.CatalogTransition;
import trading.reference.GridId;
import trading.reference.GridVersion;

public abstract class SharedReferenceDataJavaSetup {
  @SuppressWarnings("unchecked")
  protected static <E, A> A right(Either<E, A> value) {
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

  protected static CatalogTransition commitAsset(CatalogState state, AssetDefinition definition) {
    CatalogCommand command = RegisterAsset.apply(definition);
    return right(CatalogModel.commit(state, CatalogBatch.one(command)));
  }
}
