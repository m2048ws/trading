package external.referencejava;

import java.lang.reflect.Modifier;
import trading.quantity.Dim;
import trading.reference.Asset;
import trading.reference.AssetDefinition;
import trading.reference.CatalogRoot;
import trading.reference.CatalogTransition;
import trading.reference.DimensionHandle;
import trading.reference.GridHandle;

public final class HandleConstructionBoundary extends SharedReferenceDataJavaSetup {
  public static void main(String[] args) {
    AssetDefinition definition = assetDefinition("EUR");
    CatalogTransition transition = commitAsset(CatalogRoot.create().initialState(), definition);
    Asset issuedAsset = right(transition.state().snapshot().resolveAsset(definition.id()));

    requireFinalValue(DimensionHandle.class);
    requireFinalValue(Asset.class);
    requireFinalValue(GridHandle.class);
    if (issuedAsset.dimension().key() == null) {
      throw new AssertionError("catalog-issued dimension was unavailable");
    }

    @SuppressWarnings("unchecked")
    DimensionHandle<Dim> dimension = (DimensionHandle<Dim>) issuedAsset.dimension();
    rejectsConstruction(() -> new DimensionHandle<>(new Object(), new Object(), dimension.ref()));
    rejectsConstruction(() -> new Asset(new Object(), new Object(), issuedAsset.id(), dimension));
    rejectsConstruction(() -> new GridHandle<>(new Object(), new Object(), null, dimension, null));
  }

  private static void requireFinalValue(Class<?> handleClass) {
    if (handleClass.isInterface() || !Modifier.isFinal(handleClass.getModifiers())) {
      throw new AssertionError(handleClass.getName() + " must be a final value class");
    }
  }

  private static void rejectsConstruction(Runnable attempt) {
    try {
      attempt.run();
      throw new AssertionError("caller construction returned trusted authority");
    } catch (IllegalArgumentException expected) {
      // The inaccessible catalog permit is required before any handle can be valid.
    }
  }
}
