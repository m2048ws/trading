package external.reference.positive

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.reference.*

object CatalogOutcomeInspectionBoundary:
  private val (publishedState, publishedOutcome) = transition match
    case CatalogTransition(state, outcome @ CatalogCommit.Published(publishedSnapshot, delta)) =>
      assert(state.snapshot.revision == publishedSnapshot.revision)
      assert(delta.additions.distinct.size == delta.additions.size)
      state -> outcome
    case other => throw new AssertionError(s"expected model-issued publication, got $other")

  assert(publishedState eq transition.state)
  assert(publishedOutcome eq transition.outcome)

  private val retry = CatalogModel
    .commit(publishedState, batch)
    .fold(error => throw new AssertionError(error.toString), identity)

  retry match
    case CatalogTransition(state, CatalogCommit.Unchanged(unchangedSnapshot)) =>
      assert(state eq publishedState)
      assert(unchangedSnapshot.revision == publishedState.revision)
    case other => throw new AssertionError(s"expected model-issued unchanged result, got $other")

  val validDelta = CatalogDelta
    .from(
      Vector(
        CatalogAddition.Dimension(asset.dimension.key),
        CatalogAddition.Asset(asset.id),
        CatalogAddition.Grid(grid.identity)
      )
    )
    .fold(error => throw new AssertionError(error.toString), identity)

  assert(validDelta.additions.size == 3)

  private def rejectsDuplicateDelta(): Unit =
    val addition = CatalogAddition.Asset(asset.id)
    assert(CatalogDelta.from(Vector(addition, addition)) == Left(DuplicateCatalogAddition(addition)))

  rejectsDuplicateDelta()
end CatalogOutcomeInspectionBoundary
