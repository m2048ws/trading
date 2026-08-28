package external.referencejava;

import java.lang.reflect.Modifier;
import trading.quantity.Dim;
import trading.quantity.Rational;
import trading.reference.Asset;
import trading.reference.AssetDefinition;
import trading.reference.DimensionHandle;
import trading.reference.GridDefinition;
import trading.reference.GridHandle;
import trading.reference.GridIdentity;
import trading.reference.GridKey;
import trading.reference.QuantityRegistry;

public final class HandleConstructionBoundary extends SharedReferenceDataJavaSetup {
  public static void main(String[] args) {
    QuantityRegistry registry = new QuantityRegistry();
    AssetDefinition definition = assetDefinition("EUR");
    Asset issuedAsset = right(registry.registerAsset(definition));

    @SuppressWarnings("unchecked")
    DimensionHandle<Dim> issuedDimension = (DimensionHandle<Dim>) issuedAsset.dimension();
    GridIdentity identity =
        new GridIdentity(
            issuedDimension.key(), new GridKey(gridId("cent"), gridVersion(1)));
    GridHandle<Dim> issuedGrid =
        right(
            registry.registerGrid(
                issuedDimension, right(GridDefinition.from(identity, Rational.one()))));
    Object observedLineage =
        issuedDimension.trading$reference$DimensionHandle$$lineageToken();

    requireFinalValue(DimensionHandle.class);
    requireFinalValue(Asset.class);
    requireFinalValue(GridHandle.class);

    rejectsConstruction(
        () -> new DimensionHandle<>(new Object(), observedLineage, issuedDimension.ref()));
    rejectsConstruction(
        () -> new Asset(new Object(), observedLineage, issuedAsset.id(), issuedDimension));
    rejectsConstruction(
        () ->
            new GridHandle<>(
                new Object(),
                observedLineage,
                issuedGrid.identity(),
                issuedDimension,
                issuedGrid.grid()));
  }

  private static void requireFinalValue(Class<?> handleClass) {
    if (handleClass.isInterface() || !Modifier.isFinal(handleClass.getModifiers())) {
      throw new AssertionError(handleClass.getName() + " must be a final value class");
    }
  }

  private static void rejectsConstruction(Runnable attempt) {
    try {
      attempt.run();
      throw new AssertionError("client-defined handle construction returned a value");
    } catch (IllegalArgumentException expected) {
      // The registry owns the only accepted construction argument.
    }
  }
}
