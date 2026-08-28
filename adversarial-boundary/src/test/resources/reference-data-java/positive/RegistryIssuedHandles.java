package external.referencejava;

import scala.util.Either;
import trading.quantity.Rational;
import trading.reference.Asset;
import trading.reference.AssetDefinition;
import trading.reference.GridDefinition;
import trading.reference.GridHandle;
import trading.reference.GridIdentity;
import trading.reference.GridKey;
import trading.reference.QuantityRegistry;

public final class RegistryIssuedHandles extends SharedReferenceDataJavaSetup {
  public static void main(String[] args) {
    QuantityRegistry registry = new QuantityRegistry();
    AssetDefinition assetDefinition = assetDefinition("USD");
    Asset asset = right(registry.registerAsset(assetDefinition));
    GridIdentity identity =
        new GridIdentity(
            asset.dimension().key(), new GridKey(gridId("cent"), gridVersion(1)));
    GridDefinition gridDefinition = right(GridDefinition.from(identity, Rational.one()));
    GridHandle<?> grid = right(registry.registerGrid(asset.dimension(), gridDefinition));

    Either<?, ?> sameAsset = Asset.reconcile(asset, asset);
    Either<?, ?> sameGrid = GridHandle.reconcile(grid, grid);
    if (sameAsset.isLeft() || sameGrid.isLeft()) {
      throw new AssertionError("registry-issued handles did not reconcile");
    }
  }
}
