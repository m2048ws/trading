package trading.reference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import trading.quantity.AtomId;
import trading.quantity.Dim;
import trading.quantity.DimKey;
import trading.quantity.DimRef;
import trading.quantity.Rational;
import trading.quantity.UniformGrid;

/** JVM-enforced implementation boundary for the transitional Scala registry facade. */
abstract class QuantityRegistryKernel {
  private static final Object HANDLE_PERMIT = new Object();

  private final Object lineage = new Object();
  private final Map<AssetId, AssetEntry> assets = new HashMap<>();
  private final Map<DimKey, DimensionHandle<? extends Dim>> dimensions = new HashMap<>();
  private final Map<DimKey, Map<GridKey, GridEntry>> grids = new HashMap<>();

  static boolean isHandlePermit(Object candidate) {
    return candidate == HANDLE_PERMIT;
  }

  protected final synchronized Either<ReferenceDataError, Asset> kernelRegisterAsset(
      AssetDefinition definition) {
    AssetDefinition checked = Objects.requireNonNull(definition, "asset definition");
    AssetEntry existing = assets.get(checked.id());
    if (existing != null) {
      return existing.atom.equals(checked.dimensionAtom())
          ? right(existing.asset)
          : left(
              new ConflictingAssetDefinition(
                  checked.id(), existing.atom, checked.dimensionAtom()));
    }

    DimKey dimensionKey = DimKey.atom(checked.dimensionAtom());
    if (dimensions.containsKey(dimensionKey)) {
      return left(new ConflictingDimensionRegistration(dimensionKey));
    }

    DimRef<?> generated = DimRef.atomic(checked.dimensionAtom()).dimension();
    DimensionHandle<?> dimension = new DimensionHandle<>(HANDLE_PERMIT, lineage, generated);
    Asset asset = new Asset(HANDLE_PERMIT, lineage, checked.id(), dimension);
    assets.put(checked.id(), new AssetEntry(checked.dimensionAtom(), asset));
    dimensions.put(dimension.key(), dimension);
    return right(asset);
  }

  protected final synchronized Either<ReferenceDataError, Asset> kernelResolveAsset(AssetId id) {
    AssetId checked = Objects.requireNonNull(id, "asset ID");
    AssetEntry entry = assets.get(checked);
    return entry == null ? left(new UnknownAsset(checked)) : right(entry.asset);
  }

  protected final synchronized Either<ReferenceDataError, DimensionHandle<? extends Dim>>
      kernelRegisterDimension(DimKey key) {
    DimKey checked = Objects.requireNonNull(key, "dimension key");
    DimensionHandle<? extends Dim> existing = dimensions.get(checked);
    if (existing != null) {
      return right(existing);
    }

    DimRef<?> generated = DimRef.fresh(checked).dimension();
    DimensionHandle<?> dimension = new DimensionHandle<>(HANDLE_PERMIT, lineage, generated);
    dimensions.put(checked, dimension);
    return right(dimension);
  }

  protected final synchronized Either<ReferenceDataError, DimensionHandle<? extends Dim>>
      kernelResolveDimension(DimKey key) {
    DimKey checked = Objects.requireNonNull(key, "dimension key");
    DimensionHandle<? extends Dim> handle = dimensions.get(checked);
    return handle == null ? left(new UnknownDimension(checked)) : right(handle);
  }

  protected final synchronized <D extends Dim>
      Either<ReferenceDataError, GridHandle<D>> kernelRegisterGrid(
          DimensionHandle<D> dimension, GridDefinition definition) {
    DimensionHandle<D> checkedDimension =
        Objects.requireNonNull(dimension, "dimension handle");
    GridDefinition checkedDefinition = Objects.requireNonNull(definition, "grid definition");
    if (!isCanonical(checkedDimension)) {
      return left(new ForeignDimensionHandle(checkedDimension.key()));
    }
    if (!checkedDefinition.dimension().equals(checkedDimension.key())) {
      return left(
          new GridDefinitionDimensionMismatch(
              checkedDimension.key(), checkedDefinition.dimension()));
    }

    Map<GridKey, GridEntry> dimensionGrids =
        grids.computeIfAbsent(checkedDimension.key(), ignored -> new HashMap<>());
    GridEntry existing = dimensionGrids.get(checkedDefinition.key());
    if (existing != null) {
      return existing.quantum.equals(checkedDefinition.quantum())
          ? right(gridAtCanonicalDimension(existing.handle))
          : left(
              new ConflictingGridDefinition(
                  checkedDefinition.identity(), existing.quantum, checkedDefinition.quantum()));
    }

    Object grid = UniformGrid.create(checkedDimension.ref(), checkedDefinition.quantum());
    GridHandle<D> handle =
        new GridHandle<>(
            HANDLE_PERMIT, lineage, checkedDefinition.identity(), checkedDimension, grid);
    dimensionGrids.put(
        checkedDefinition.key(), new GridEntry(checkedDefinition.quantum(), handle));
    return right(handle);
  }

  protected final synchronized <D extends Dim>
      Either<ReferenceDataError, GridHandle<D>> kernelResolveGrid(
          DimensionHandle<D> dimension, GridKey key) {
    DimensionHandle<D> checkedDimension =
        Objects.requireNonNull(dimension, "dimension handle");
    GridKey checkedKey = Objects.requireNonNull(key, "grid key");
    if (!isCanonical(checkedDimension)) {
      return left(new ForeignDimensionHandle(checkedDimension.key()));
    }

    Map<GridKey, GridEntry> dimensionGrids = grids.get(checkedDimension.key());
    GridEntry entry = dimensionGrids == null ? null : dimensionGrids.get(checkedKey);
    return entry == null
        ? left(new UnknownGrid(checkedDimension.key(), checkedKey))
        : right(gridAtCanonicalDimension(entry.handle));
  }

  protected final synchronized int kernelRegisteredAssetCount() {
    return assets.size();
  }

  protected final synchronized int kernelRegisteredDimensionCount() {
    return dimensions.size();
  }

  protected final synchronized int kernelRegisteredGridCount() {
    return grids.values().stream().mapToInt(Map::size).sum();
  }

  private boolean isCanonical(DimensionHandle<?> handle) {
    return dimensions.get(handle.key()) == handle;
  }

  @SuppressWarnings("unchecked")
  private static <D extends Dim> GridHandle<D> gridAtCanonicalDimension(
      GridHandle<? extends Dim> handle) {
    return (GridHandle<D>) handle;
  }

  private static <A> Either<ReferenceDataError, A> right(A value) {
    return new Right<>(value);
  }

  private static <A> Either<ReferenceDataError, A> left(ReferenceDataError error) {
    return new Left<>(error);
  }

  private record AssetEntry(AtomId atom, Asset asset) {}

  private record GridEntry(Rational quantum, GridHandle<? extends Dim> handle) {}

}
