package external.referencejava;

import scala.util.Either;
import trading.quantity.DimRef;
import trading.quantity.Rational;
import trading.quantity.UniformGrid;
import trading.reference.Asset;
import trading.reference.GridDefinition;
import trading.reference.GridHandle;
import trading.reference.GridIdentity;
import trading.reference.GridKey;
import trading.reference.QuantityRegistry;
import trading.reference.ReferenceDataError;

public final class GridQuantumConstructionBoundary extends SharedReferenceDataJavaSetup {
  public static void main(String[] arguments) {
    QuantityRegistry registry = new QuantityRegistry();
    Asset asset = right(registry.registerAsset(assetDefinition("checked-grid-quantum")));
    GridIdentity identity =
        new GridIdentity(
            asset.dimension().key(), new GridKey(gridId("checked"), gridVersion(1)));

    Either<? extends ReferenceDataError, GridDefinition> zeroDefinition =
        GridDefinition.from(identity, Rational.zero());
    Either<? extends ReferenceDataError, GridDefinition> negativeDefinition =
        GridDefinition.from(identity, Rational.one().unary_$minus());
    if (!zeroDefinition.isLeft() || !negativeDefinition.isLeft()) {
      throw new AssertionError("nonpositive raw grid definitions must be rejected");
    }

    if (!UniformGrid.from(DimRef.one(), Rational.zero()).isLeft()
        || !UniformGrid.from(DimRef.one(), Rational.one().unary_$minus()).isLeft()) {
      throw new AssertionError("nonpositive raw uniform grids must be rejected");
    }

    rejectsErasedConstruction(() -> GridDefinition.apply(identity, Rational.zero()));
    rejectsErasedConstruction(() -> UniformGrid.create(DimRef.one(), Rational.zero()));
    rejectsErasedConstruction(
        () -> new GridDefinition(new Object(), identity, Rational.one()));

    GridDefinition positiveDefinition = right(GridDefinition.from(identity, Rational.one()));
    GridHandle<?> positiveHandle =
        right(registry.registerGrid(asset.dimension(), positiveDefinition));
    if (positiveHandle.quantum().compare(Rational.zero()) <= 0) {
      throw new AssertionError("positive checked grid issuance lost its refinement");
    }
    if (UniformGrid.create(DimRef.one(), Rational.one()) == null) {
      throw new AssertionError("positive erased uniform-grid construction must remain supported");
    }
  }

  private static void rejectsErasedConstruction(Runnable attempt) {
    try {
      attempt.run();
      throw new AssertionError("erased nonpositive construction returned grid authority");
    } catch (IllegalArgumentException expected) {
      // Raw/JVM callers use the typed checked factories; erased refined entry points fail closed.
    }
  }
}
