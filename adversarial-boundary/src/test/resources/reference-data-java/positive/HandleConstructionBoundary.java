package external.referencejava;

import trading.reference.Asset;
import trading.reference.AssetDefinition;
import trading.reference.CatalogRoot;
import trading.reference.CatalogTransition;

public final class HandleConstructionBoundary extends SharedReferenceDataJavaSetup {
  public static void main(String[] args) {
    AssetDefinition definition = assetDefinition("EUR");
    CatalogTransition transition = commitAsset(CatalogRoot.create().initialState(), definition);
    Asset issuedAsset = right(transition.state().snapshot().resolveAsset(definition.id()));

    if (!issuedAsset.id().equals(definition.id())
        || issuedAsset.dimension().key() == null
        || !transition.state().snapshot().resolveAsset(definition.id()).contains(issuedAsset)) {
      throw new AssertionError("catalog-issued handles did not preserve their checked identity");
    }
  }
}
