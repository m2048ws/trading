package external.reference.negative

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.reference.*

object CatalogObservationConstruction:
  val published = transition.outcome match
    case value: CatalogCommit.Published => value
    case other                          => throw new AssertionError(other.toString)

  // OFFENDING-BEGIN
  val unchanged       = CatalogCommit.Unchanged(snapshot)
  val inventedPublish = CatalogCommit.Published(snapshot, published.delta)
  val inventedResult  = CatalogTransition(transition.state, transition.outcome)
  val copiedPublish   = published.copy(delta = published.delta)
  val copiedResult    = transition.copy(outcome = transition.outcome)
  // OFFENDING-END

end CatalogObservationConstruction
