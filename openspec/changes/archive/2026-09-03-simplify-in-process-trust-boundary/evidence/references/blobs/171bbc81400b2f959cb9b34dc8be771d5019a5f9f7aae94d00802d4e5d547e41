package external.referencejava;

import scala.math.BigInt;
import trading.quantity.Rational;
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

public final class GridReconciliationAuthority extends SharedReferenceDataJavaSetup {
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void main(String[] arguments) {
    AssetDefinition definition = assetDefinition("reconciliation-authority");
    CatalogTransition assetTransition =
        commitAsset(CatalogRoot.create().initialState(), definition);
    Asset asset = right(assetTransition.state().snapshot().resolveAsset(definition.id()));
    GridIdentity identity =
        new GridIdentity(
            asset.dimension().key(),
            new GridKey(gridId("reconciliation-grid"), gridVersion(1)));
    GridDefinition gridDefinition = right(GridDefinition.from(identity, Rational.one()));
    CatalogTransition gridTransition =
        right(
            CatalogModel.commit(
                assetTransition.state(),
                CatalogBatch.one(RegisterGrid.apply(gridDefinition))));
    GridHandle handle =
        right(gridTransition.state().snapshot().resolveGrid(gridDefinition.identity()));

    Object checked = right(GridHandle.reconcile(handle, handle));
    BigInt coordinate = BigInt.apply(23);
    BigInt retyped = GridHandle.retype(checked, coordinate);
    if (!coordinate.equals(retyped)) {
      throw new AssertionError("checked reconciliation did not preserve the coordinate");
    }

    try {
      GridHandle.retype(null, coordinate);
      throw new AssertionError("null reconciliation evidence was accepted");
    } catch (NullPointerException expected) {
      // Ordinary erased Java misuse still fails at the supported operation boundary.
    }
  }
}
