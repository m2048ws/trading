package external.referencejava;

import scala.math.BigInt;
import trading.quantity.AtomId;
import trading.quantity.DimKey;
import trading.quantity.Rational;
import trading.reference.AssetDefinition;
import trading.reference.CatalogViolation;
import trading.reference.CatalogViolation.AssetDimensionAlreadyBound;
import trading.reference.CatalogViolation.ImmutableAssetConflict;
import trading.reference.CatalogViolation.ImmutableGridConflict;
import trading.reference.CatalogViolation.MissingGridDimension;
import trading.reference.ForeignDimensionHandle;
import trading.reference.GridIdentity;
import trading.reference.GridKey;
import trading.reference.IndexedCatalogViolation;
import trading.reference.NegativeCatalogRevision;
import trading.reference.UnknownAsset;
import trading.reference.UnknownDimension;
import trading.reference.UnknownGrid;

public final class CatalogErrorConstructionBoundary extends SharedReferenceDataJavaSetup {
  public static void main(String[] arguments) {
    AssetDefinition first = assetDefinition("java-violation-first");
    AssetDefinition second = new AssetDefinition(first.id(), new AtomId("java:violation-second"));
    AssetDefinition other = assetDefinition("java-violation-other");
    DimKey dimension = DimKey.atom(first.dimensionAtom());
    GridIdentity gridIdentity =
        new GridIdentity(dimension, new GridKey(gridId("java-violation-grid"), gridVersion(1)));
    DimKey composite = DimKey.multiply(dimension, DimKey.atom(other.dimensionAtom()));

    rejectsNull(() -> new NegativeCatalogRevision(null));
    rejectsNull(() -> new UnknownAsset(null));
    rejectsNull(() -> new UnknownDimension(null));
    rejectsNull(() -> new UnknownGrid(null));
    rejectsNull(() -> new ForeignDimensionHandle(null));
    rejectsMalformed(() -> new NegativeCatalogRevision(BigInt.apply(0)));
    rejectsMalformed(() -> ImmutableAssetConflict.apply(first.id(), first, first));
    rejectsMalformed(() -> ImmutableAssetConflict.apply(first.id(), other, second));
    rejectsMalformed(() -> ImmutableGridConflict.apply(gridIdentity, Rational.zero(), Rational.one()));
    rejectsMalformed(() -> ImmutableGridConflict.apply(gridIdentity, Rational.one(), Rational.one().unary_$minus()));
    rejectsMalformed(() -> AssetDimensionAlreadyBound.apply(dimension, first.id(), first.id()));
    rejectsMalformed(() -> AssetDimensionAlreadyBound.apply(DimKey.one(), first.id(), other.id()));
    rejectsMalformed(() -> AssetDimensionAlreadyBound.apply(composite, first.id(), other.id()));
    CatalogViolation missing = MissingGridDimension.apply(gridIdentity);
    rejectsMalformed(() -> new IndexedCatalogViolation(0, 99, missing));

    ImmutableAssetConflict validAssetConflict = ImmutableAssetConflict.apply(first.id(), first, second);
    ImmutableGridConflict validGridConflict =
        ImmutableGridConflict.apply(
            gridIdentity,
            Rational.apply(BigInt.apply(1), BigInt.apply(100)),
            Rational.apply(BigInt.apply(1), BigInt.apply(1000)));
    AssetDimensionAlreadyBound validBindingConflict =
        AssetDimensionAlreadyBound.apply(dimension, first.id(), other.id());
    IndexedCatalogViolation validIndexed = new IndexedCatalogViolation(0, 3, missing);
    if (!validAssetConflict.id().equals(first.id())
        || !validGridConflict.identity().equals(gridIdentity)
        || !validBindingConflict.dimension().equals(dimension)
        || validIndexed.ruleOrdinal() != 3) {
      throw new AssertionError("valid catalog conflict evidence was not retained");
    }
  }

  private static void rejectsNull(Runnable attempt) {
    try {
      attempt.run();
      throw new AssertionError("null catalog error payload was retained");
    } catch (NullPointerException expected) {
      // Null is unsupported input, not a domain error payload.
    }
  }

  private static void rejectsMalformed(Runnable attempt) {
    try {
      attempt.run();
      throw new AssertionError("malformed catalog error value was retained");
    } catch (IllegalArgumentException expected) {
      // NegativeCatalogRevision can only describe the typed negative-input result.
    }
  }
}
