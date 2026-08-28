package trading.reference

import external.reference.fixtures.SharedReferenceDataSetup.*

object InternalImplementationAccess:
  // OFFENDING-BEGIN
  val lineage = new QuantityRegistry.Lineage
  val dimension = new QuantityRegistry.InternedDimensionHandle(lineage, asset.dimension.ref)
  val internedAsset = new QuantityRegistry.InternedAsset(lineage, asset.id, asset.dimension)
  val internedGrid =
    new QuantityRegistry.InternedGridHandle(lineage, grid.identity, grid.dimension, grid.grid)
  // OFFENDING-END

end InternalImplementationAccess
