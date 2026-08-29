package external.referencejava;

import scala.util.Either;
import trading.quantity.DimRef;
import trading.quantity.Rational;
import trading.quantity.UniformGrid;
import trading.reference.Asset;
import trading.reference.AssetDefinition;
import trading.reference.CatalogBatch;
import trading.reference.CatalogCommand;
import trading.reference.CatalogCommand.RegisterGrid;
import trading.reference.CatalogModel;
import trading.reference.CatalogRoot;
import trading.reference.CatalogTransition;
import trading.reference.GridDefinition;
import trading.reference.GridHandle;
import trading.reference.GridIdentity;
import trading.reference.GridKey;

public final class GridQuantumConstructionBoundary extends SharedReferenceDataJavaSetup {
  public static void main(String[] arguments) {
    AssetDefinition assetDefinition = assetDefinition("checked-grid-quantum");
    CatalogTransition assetTransition =
        commitAsset(CatalogRoot.create().initialState(), assetDefinition);
    Asset asset = right(assetTransition.state().snapshot().resolveAsset(assetDefinition.id()));
    GridIdentity identity =
        new GridIdentity(
            asset.dimension().key(), new GridKey(gridId("checked"), gridVersion(1)));

    Either<?, GridDefinition> zeroDefinition = GridDefinition.from(identity, Rational.zero());
    Either<?, GridDefinition> negativeDefinition =
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

    GridDefinition positiveDefinition = right(GridDefinition.from(identity, Rational.one()));
    CatalogCommand gridCommand = RegisterGrid.apply(positiveDefinition);
    CatalogTransition gridTransition =
        right(
            CatalogModel.commit(
                assetTransition.state(), CatalogBatch.one(gridCommand)));
    GridHandle<?> positiveHandle =
        right(gridTransition.state().snapshot().resolveGrid(positiveDefinition.identity()));
    if (positiveHandle.quantum().compare(Rational.zero()) <= 0) {
      throw new AssertionError("positive checked grid issuance lost its refinement");
    }
  }

  private static void rejectsErasedConstruction(Runnable attempt) {
    try {
      attempt.run();
      throw new AssertionError("erased nonpositive construction returned grid authority");
    } catch (IllegalArgumentException expected) {
      // Raw/JVM callers use typed checked factories; erased refined entry points fail closed.
    }
  }
}
