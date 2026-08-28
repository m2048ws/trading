package trading.reference

import external.reference.fixtures.SharedReferenceDataSetup.*

object InternalImplementationAccess:
  // OFFENDING-BEGIN
  val permit = CatalogState.handlePermit
  val lineage = transition.state.lineage
  val state = new CatalogState(permit, lineage, CatalogRevision.zero, Map.empty, Map.empty, Map.empty, Map.empty, Map.empty)
  val snapshot = new CatalogSnapshot(permit, lineage, CatalogRevision.zero, Map.empty, Map.empty, Map.empty)
  val evidence = new GridHandle.Reconciliation[asset.D, grid.G, asset.D, grid.G]
  // OFFENDING-END

end InternalImplementationAccess
