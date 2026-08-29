package external.reference.negative

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.reference.*

object CatalogOutcomeConstruction:
  private val published = transition.outcome match
    case value: CatalogCommit.Published => value
    case other                          => throw new AssertionError(s"expected publication, got $other")

  // OFFENDING-BEGIN
  val forgedUnchanged = CatalogCommit.Unchanged(CatalogRoot.create().initialState.snapshot)
  val forgedPublished = CatalogCommit.Published(CatalogRoot.create().initialState.snapshot, published.delta)
  val forgedTransition = CatalogTransition(CatalogRoot.create().initialState, forgedPublished)
  // OFFENDING-END
end CatalogOutcomeConstruction
