package trading.application

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

/** Reusable semantic contract for Proposal 8 and any later live-catalog interpreter. */
abstract class LiveCatalogContract[F[_]] extends FunSuite:
  protected def live(initial: CatalogState): LiveCatalog[F]
  protected def run[A](effect: F[A]): A
  protected def concurrently[A, B](left: F[A], right: F[B]): (A, B)

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => throw new AssertionError(error.toString), identity)

  private[application] final def lineageSeedAsset: AssetDefinition =
    AssetDefinition(required(AssetId.from("contract-initial")), AtomId("contract:initial"))

  private[application] final def lineageSeedGrid: GridDefinition =
    val asset = lineageSeedAsset
    GridDefinition(
      GridIdentity(
        DimKey.atom(asset.dimensionAtom),
        GridKey(required(GridId.from("contract-initial-grid")), required(GridVersion.from(1)))
      ),
      required(PositiveRational.exact(1, 100))
    )

  private[application] final def lineageSeedBatch: CatalogBatch =
    CatalogBatch.of(
      CatalogCommand.RegisterAsset(lineageSeedAsset),
      CatalogCommand.RegisterGrid(lineageSeedGrid)
    )

  private def definition(name: String, atom: String = ""): AssetDefinition =
    AssetDefinition(
      AssetId.from(name).toOption.get,
      AtomId(if atom.isEmpty then s"contract:$name" else atom)
    )

  private def batch(definition: AssetDefinition): CatalogBatch =
    CatalogBatch.one(CatalogCommand.RegisterAsset(definition))

  private def pureOutcome(
    state: CatalogState,
    definition: AssetDefinition
  ): Either[CatalogViolations, CatalogCommit] =
    CatalogModel.commit(state, batch(definition)).map(_.outcome)

  private def assertSnapshotShape(actual: CatalogSnapshot, expected: CatalogSnapshot): Unit =
    assertEquals(actual.revision, expected.revision)
    assertEquals(actual.assetCount, expected.assetCount)
    assertEquals(actual.dimensionCount, expected.dimensionCount)
    assertEquals(actual.gridCount, expected.gridCount)

  private def assertAsset(snapshot: CatalogSnapshot, expected: AssetDefinition): Unit =
    val resolved = snapshot.resolveAsset(expected.id)
    assert(resolved.isRight, resolved)
    val asset = resolved.toOption.get
    assertEquals(asset.id, expected.id)
    assertEquals(asset.dimension.key, DimKey.atom(expected.dimensionAtom))

  private def asset(snapshot: CatalogSnapshot, id: AssetId): Asset =
    snapshot.resolveAsset(id).fold(error => fail(s"expected asset $id, got $error"), identity)

  private def grid(snapshot: CatalogSnapshot, identity: GridIdentity): GridHandle[? <: Dim] =
    snapshot.resolveGrid(identity).fold(error => fail(s"expected grid $identity, got $error"), value => value)

  private def assertAssetReconciles(
    left: CatalogSnapshot,
    right: CatalogSnapshot,
    id: AssetId
  ): Unit =
    val leftAsset   = asset(left, id)
    val rightAsset  = asset(right, id)
    val assetResult = Asset.reconcile(leftAsset, rightAsset)
    assert(assetResult.isRight, s"asset $id did not reconcile across catalog generations: $assetResult")
    val dimensionResult = DimensionHandle.reconcile(leftAsset.dimension, rightAsset.dimension)
    assert(dimensionResult.isRight,
      s"asset dimension $id did not reconcile across catalog generations: $dimensionResult")

  private def assertGridReconciles(
    left: CatalogSnapshot,
    right: CatalogSnapshot,
    identity: GridIdentity
  ): Unit =
    val leftGrid   = grid(left, identity)
    val rightGrid  = grid(right, identity)
    val gridResult = GridHandle.reconcile(leftGrid, rightGrid)
    assert(gridResult.isRight, s"grid $identity did not reconcile across catalog generations: $gridResult")
    val dimensionResult = DimensionHandle.reconcile(leftGrid.dimension, rightGrid.dimension)
    assert(dimensionResult.isRight,
      s"grid dimension $identity did not reconcile across catalog generations: $dimensionResult")

  private def assertAssetGridRelationship(
    snapshot: CatalogSnapshot,
    assetId: AssetId,
    gridIdentity: GridIdentity
  ): Unit =
    val assetValue = asset(snapshot, assetId)
    val gridValue  = grid(snapshot, gridIdentity)
    val result     = DimensionHandle.reconcile(assetValue.dimension, gridValue.dimension)
    assert(result.isRight, s"asset/grid dimension relationship did not reconcile in one snapshot: $result")

  private def assertOutcomeEquivalent(
    actual: Either[CatalogViolations, CatalogCommit],
    expected: Either[CatalogViolations, CatalogCommit]
  ): Unit =
    (actual, expected) match
      case (Left(actualErrors), Left(expectedErrors)) =>
        assertEquals(actualErrors, expectedErrors)
      case (Right(CatalogCommit.Unchanged(actualSnapshot)), Right(CatalogCommit.Unchanged(expectedSnapshot))) =>
        assertSnapshotShape(actualSnapshot, expectedSnapshot)
      case (
          Right(CatalogCommit.Published(actualSnapshot, actualDelta)),
          Right(CatalogCommit.Published(expectedSnapshot, expectedDelta))
        ) =>
        assertSnapshotShape(actualSnapshot, expectedSnapshot)
        assertEquals(actualDelta, expectedDelta)
      case _ =>
        fail(s"live outcome $actual did not match pure outcome $expected")

  private[application] final def assertSequentialModelEquivalence(): Unit =
    val initialAsset     = lineageSeedAsset
    val initialGrid      = lineageSeedGrid
    val initial          = required(CatalogModel.commit(CatalogRoot.create().initialState, lineageSeedBatch)).state
    val first            = definition("sequential-first")
    val second           = definition("sequential-second")
    val conflictingFirst = AssetDefinition(first.id, AtomId("contract:sequential-conflict"))
    val expectedFirst    = CatalogModel.commit(initial, batch(first)).toOption.get
    val expectedSecond   = CatalogModel.commit(expectedFirst.state, batch(second)).toOption.get
    val expectedConflict = pureOutcome(expectedSecond.state, conflictingFirst)
    val expectedRetry    = pureOutcome(expectedSecond.state, second)
    val catalog          = live(initial)

    val actualInitial = run(catalog.snapshot)
    assertSnapshotShape(actualInitial, initial.snapshot)
    assertAssetReconciles(actualInitial, initial.snapshot, initialAsset.id)
    assertGridReconciles(actualInitial, initial.snapshot, initialGrid.identity)
    assertAssetGridRelationship(actualInitial, initialAsset.id, initialGrid.identity)

    val actualFirst = run(catalog.commit(batch(first)))
    assertOutcomeEquivalent(actualFirst, Right(expectedFirst.outcome))
    val actualFirstCommit = actualFirst.toOption.get
    assertAsset(actualFirstCommit.snapshot, first)
    assertAssetReconciles(actualInitial, actualFirstCommit.snapshot, initialAsset.id)
    assertGridReconciles(actualInitial, actualFirstCommit.snapshot, initialGrid.identity)
    assertAssetGridRelationship(actualFirstCommit.snapshot, initialAsset.id, initialGrid.identity)

    val actualSecond = run(catalog.commit(batch(second)))
    assertOutcomeEquivalent(actualSecond, Right(expectedSecond.outcome))
    val actualSecondCommit = actualSecond.toOption.get
    assertAsset(actualSecondCommit.snapshot, first)
    assertAsset(actualSecondCommit.snapshot, second)
    assertAssetReconciles(actualFirstCommit.snapshot, actualSecondCommit.snapshot, initialAsset.id)
    assertAssetReconciles(actualFirstCommit.snapshot, actualSecondCommit.snapshot, first.id)
    assertGridReconciles(actualFirstCommit.snapshot, actualSecondCommit.snapshot, initialGrid.identity)
    assertAssetGridRelationship(actualSecondCommit.snapshot, initialAsset.id, initialGrid.identity)

    val actualConflict = run(catalog.commit(batch(conflictingFirst)))
    assert(actualConflict.isLeft, s"expected typed conflict, got $actualConflict")
    assertOutcomeEquivalent(actualConflict, expectedConflict)

    val actualRetry = run(catalog.commit(batch(second)))
    assert(actualRetry.exists(_.isInstanceOf[CatalogCommit.Unchanged]), actualRetry)
    assertOutcomeEquivalent(actualRetry, expectedRetry)
    assertAssetReconciles(actualSecondCommit.snapshot, actualRetry.toOption.get.snapshot, first.id)
    assertGridReconciles(actualSecondCommit.snapshot, actualRetry.toOption.get.snapshot, initialGrid.identity)

    val finalSnapshot = run(catalog.snapshot)
    assertSnapshotShape(finalSnapshot, expectedSecond.state.snapshot)
    assertAsset(finalSnapshot, first)
    assertAsset(finalSnapshot, second)
    assertAssetReconciles(actualSecondCommit.snapshot, finalSnapshot, initialAsset.id)
    assertAssetReconciles(actualSecondCommit.snapshot, finalSnapshot, second.id)
    assertGridReconciles(actualSecondCommit.snapshot, finalSnapshot, initialGrid.identity)
  end assertSequentialModelEquivalence

  private[application] final def assertIndependentConcurrentCommits(): Unit =
    val first     = definition("independent-first")
    val second    = definition("independent-second")
    val catalog   = live(CatalogRoot.create().initialState)
    val results   = concurrently(catalog.commit(batch(first)), catalog.commit(batch(second)))
    val published = Vector(results._1, results._2).map:
      case Right(value: CatalogCommit.Published) => value
      case other                                 => fail(s"expected two published outcomes, got $other")

    assertEquals(published.map(_.snapshot.revision.value).sorted, Vector(BigInt(1), BigInt(2)))
    assert(published(0).delta.additions.contains(CatalogAddition.Asset(first.id)), published(0).delta)
    assert(published(1).delta.additions.contains(CatalogAddition.Asset(second.id)), published(1).delta)

    val snapshot = run(catalog.snapshot)
    assertEquals(snapshot.revision.value, BigInt(2))
    assertAsset(snapshot, first)
    assertAsset(snapshot, second)
  end assertIndependentConcurrentCommits

  private[application] final def assertConflictingConcurrentRevalidation(): Unit =
    val first   = definition("conflicting", "contract:first")
    val second  = AssetDefinition(first.id, AtomId("contract:second"))
    val initial = CatalogRoot.create().initialState
    val catalog = live(initial)
    val results = concurrently(catalog.commit(batch(first)), catalog.commit(batch(second)))

    def assertWinnerAndLoser(
      winner: AssetDefinition,
      loser: AssetDefinition,
      published: CatalogCommit.Published,
      losingResult: Either[CatalogViolations, CatalogCommit]
    ): Unit =
      val expectedWinner = CatalogModel.commit(initial, batch(winner)).toOption.get
      val expectedLoser  = pureOutcome(expectedWinner.state, loser)
      assertOutcomeEquivalent(Right(published), Right(expectedWinner.outcome))
      assert(losingResult.isLeft, s"expected the losing commit to return a typed conflict, got $losingResult")
      assertOutcomeEquivalent(losingResult, expectedLoser)

      val snapshot = run(catalog.snapshot)
      assertSnapshotShape(snapshot, expectedWinner.state.snapshot)
      assertAsset(snapshot, winner)

    (results._1, results._2) match
      case (Right(published: CatalogCommit.Published), losing @ Left(_)) =>
        assertWinnerAndLoser(first, second, published, losing)
      case (losing @ Left(_), Right(published: CatalogCommit.Published)) =>
        assertWinnerAndLoser(second, first, published, losing)
      case other =>
        fail(s"exactly one conflicting commit must publish and the loser must return its typed conflict: $other")
  end assertConflictingConcurrentRevalidation

  private[application] final def assertSnapshotCommitLinearization(): Unit =
    val definitionValue = definition("snapshot-linearization")
    val initial         = CatalogRoot.create().initialState
    val expected        = CatalogModel.commit(initial, batch(definitionValue)).toOption.get
    val catalog         = live(initial)
    val overlap         = concurrently(catalog.snapshot, catalog.commit(batch(definitionValue)))

    assertOutcomeEquivalent(overlap._2, Right(expected.outcome))
    overlap._1.revision.value match
      case revision if revision == initial.revision.value =>
        assertSnapshotShape(overlap._1, initial.snapshot)
        assertEquals(overlap._1.resolveAsset(definitionValue.id), Left(UnknownAsset(definitionValue.id)))
      case revision if revision == expected.state.revision.value =>
        assertSnapshotShape(overlap._1, expected.state.snapshot)
        assertAsset(overlap._1, definitionValue)
      case revision =>
        fail(s"overlapping snapshot observed non-linearized revision $revision")

    val finalSnapshot = run(catalog.snapshot)
    assertSnapshotShape(finalSnapshot, expected.state.snapshot)
    assertAsset(finalSnapshot, definitionValue)
  end assertSnapshotCommitLinearization

  test("sequential commits, conflicts, and idempotent retries agree with the pure model"):
    assertSequentialModelEquivalence()

  test("independent concurrent batches publish consecutive revisions without lost updates"):
    assertIndependentConcurrentCommits()

  test("conflicting concurrent batches revalidate and return the pure typed conflict"):
    assertConflictingConcurrentRevalidation()

  test("overlapping snapshot and commit expose one coherent linearized generation"):
    assertSnapshotCommitLinearization()
end LiveCatalogContract
