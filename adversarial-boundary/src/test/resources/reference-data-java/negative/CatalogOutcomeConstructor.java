package external.referencejava;

import trading.reference.AssetDefinition;
import trading.reference.CatalogCommit;
import trading.reference.CatalogRoot;
import trading.reference.CatalogTransition;

public final class CatalogOutcomeConstructor extends SharedReferenceDataJavaSetup {
  static final CatalogRoot root = CatalogRoot.create();
  static final AssetDefinition definition = assetDefinition("java-forged-outcome");
  static final CatalogTransition actual = commitAsset(root.initialState(), definition);
  static final CatalogCommit.Published actualPublished = (CatalogCommit.Published) actual.outcome();

  // OFFENDING-BEGIN
  static final CatalogCommit.Published forgedPublished =
      CatalogCommit.Published.apply(root.initialState().snapshot(), actualPublished.delta());
  static final CatalogTransition forgedTransition =
      new CatalogTransition(root.initialState(), forgedPublished);
  // OFFENDING-END
}
