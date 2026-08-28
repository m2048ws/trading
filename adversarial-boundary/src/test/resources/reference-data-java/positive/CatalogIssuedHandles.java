package external.referencejava;

import scala.util.Either;
import trading.reference.Asset;
import trading.reference.AssetDefinition;
import trading.reference.CatalogRoot;
import trading.reference.CatalogTransition;

public final class CatalogIssuedHandles extends SharedReferenceDataJavaSetup {
  public static void main(String[] args) {
    AssetDefinition definition = assetDefinition("USD");
    CatalogTransition transition = commitAsset(CatalogRoot.create().initialState(), definition);
    Asset asset = right(transition.state().snapshot().resolveAsset(definition.id()));

    Either<?, ?> sameAsset = Asset.reconcile(asset, asset);
    if (sameAsset.isLeft() || transition.state().revision().value().signum() != 1) {
      throw new AssertionError("catalog-issued handles did not reconcile at revision one");
    }
  }
}
