package external.referencejava;

import scala.collection.immutable.Vector;
import scala.collection.immutable.Vector$;
import trading.reference.AssetDefinition;
import trading.reference.CatalogAddition;
import trading.reference.CatalogCommit;
import trading.reference.CatalogDelta;
import trading.reference.CatalogRoot;
import trading.reference.CatalogTransition;

public final class CatalogOutcomeInspectionBoundary extends SharedReferenceDataJavaSetup {
  public static void main(String[] arguments) {
    CatalogRoot root = CatalogRoot.create();
    AssetDefinition definition = assetDefinition("java-outcome");
    CatalogTransition transition = commitAsset(root.initialState(), definition);
    CatalogCommit.Published published = (CatalogCommit.Published) transition.outcome();

    if (published.snapshot().revision().value().signum() != 1
        || !published.delta().additions().distinct().equals(published.delta().additions())) {
      throw new AssertionError("model-issued publication was not coherently inspectable");
    }

    CatalogAddition.Asset addition = CatalogAddition.Asset.apply(definition.id());
    Vector<CatalogAddition> duplicateAdditions = Vector.fillSparse(2, addition);
    if (!CatalogDelta.from(duplicateAdditions).isLeft()) {
      throw new AssertionError("checked delta factory retained duplicate additions");
    }
  }
}
