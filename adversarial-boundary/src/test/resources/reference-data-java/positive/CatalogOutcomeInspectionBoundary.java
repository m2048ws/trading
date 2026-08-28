package external.referencejava;

import scala.collection.immutable.Vector;
import scala.collection.immutable.Vector$;
import trading.quantity.DimKey;
import trading.reference.AssetDefinition;
import trading.reference.CatalogAddition;
import trading.reference.CatalogCommit;
import trading.reference.CatalogDelta;
import trading.reference.CatalogRoot;
import trading.reference.CatalogTransition;
import trading.reference.CatalogViolation;
import trading.reference.CatalogViolation.MissingGridDimension;
import trading.reference.CatalogViolations;
import trading.reference.GridIdentity;
import trading.reference.GridKey;
import trading.reference.IndexedCatalogViolation;

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

    rejectsMalformed(
        () -> new CatalogCommit.Published(new Object(), root.initialState().snapshot(), published.delta()));
    rejectsMalformed(
        () -> new CatalogTransition(new Object(), root.initialState(), published));

    CatalogAddition.Asset addition = CatalogAddition.Asset.apply(definition.id());
    Vector<CatalogAddition> duplicateAdditions = Vector.fillSparse(2, addition);
    Vector<CatalogAddition> duplicateTail = Vector.fillSparse(1, addition);
    if (!CatalogDelta.from(duplicateAdditions).isLeft()) {
      throw new AssertionError("checked delta factory retained duplicate additions");
    }
    rejectsMalformed(() -> new CatalogDelta(addition, duplicateTail));

    GridIdentity missingIdentity =
        new GridIdentity(
            DimKey.atom(definition.dimensionAtom()),
            new GridKey(gridId("java-missing-grid"), gridVersion(1)));
    CatalogViolation missing = MissingGridDimension.apply(missingIdentity);
    IndexedCatalogViolation later = new IndexedCatalogViolation(2, 3, missing);
    IndexedCatalogViolation earlier = new IndexedCatalogViolation(1, 3, missing);
    Vector<IndexedCatalogViolation> reversedTail = Vector.fillSparse(1, earlier);
    rejectsMalformed(() -> new CatalogViolations(new Object(), later, reversedTail));
  }

  private static void rejectsMalformed(Runnable attempt) {
    try {
      attempt.run();
      throw new AssertionError("malformed catalog product was retained");
    } catch (IllegalArgumentException expected) {
      // Unsupported constructor forgery is quarantined at the guarded product boundary.
    }
  }
}
