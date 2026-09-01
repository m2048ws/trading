package trading.application

import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

/** Framework- and runtime-neutral observable contract for every live-catalog interpreter. */
abstract class LiveCatalogContract[F[_]]:
  protected def create(bootstrap: Option[CatalogBatch]): F[Either[CatalogViolations, LiveCatalog[F]]]
  protected def delay[A](body: => A): F[A]
  protected def bind[A, B](effect: F[A])(next: A => F[B]): F[B]
  protected def concurrently[A, B](left: F[A], right: F[B]): F[(A, B)]

  extension [A](effect: F[A])
    private def flatMap[B](next: A => F[B]): F[B] = bind(effect)(next)
    private def map[B](next: A => B): F[B]        = bind(effect)(value => delay(next(value)))

  private def contractFail(message: String): Nothing =
    throw new AssertionError(message)

  private def contractAssert(condition: Boolean, clue: => Any): Unit =
    if !condition then contractFail(clue.toString)

  private def contractAssertEquals[A](actual: A, expected: A): Unit =
    if actual != expected then contractFail(s"obtained $actual, expected $expected")

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => contractFail(error.toString), identity)

  private def requiredLeft[E, A](value: Either[E, A]): E =
    value.fold(identity, success => contractFail(s"expected a typed failure, got $success"))

  private def requiredCatalog(bootstrap: Option[CatalogBatch]): F[LiveCatalog[F]] =
    create(bootstrap).map(required)

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
      required(AssetId.from(name)),
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
    contractAssertEquals(actual.revision, expected.revision)
    contractAssertEquals(actual.assetCount, expected.assetCount)
    contractAssertEquals(actual.dimensionCount, expected.dimensionCount)
    contractAssertEquals(actual.gridCount, expected.gridCount)

  private def assertAsset(snapshot: CatalogSnapshot, expected: AssetDefinition): Unit =
    val asset = required(snapshot.resolveAsset(expected.id))
    contractAssertEquals(asset.id, expected.id)
    contractAssertEquals(asset.dimension.key, DimKey.atom(expected.dimensionAtom))

  private def asset(snapshot: CatalogSnapshot, id: AssetId): Asset =
    snapshot.resolveAsset(id).fold(error => contractFail(s"expected asset $id, got $error"), identity)

  private def grid(snapshot: CatalogSnapshot, identity: GridIdentity): GridHandle[? <: Dim] =
    snapshot.resolveGrid(identity).fold(error => contractFail(s"expected grid $identity, got $error"), value => value)

  private def assertAssetReconciles(
    left: CatalogSnapshot,
    right: CatalogSnapshot,
    id: AssetId
  ): Unit =
    val leftAsset   = asset(left, id)
    val rightAsset  = asset(right, id)
    val assetResult = Asset.reconcile(leftAsset, rightAsset)
    contractAssert(assetResult.isRight, s"asset $id did not reconcile across catalog generations: $assetResult")
    val dimensionResult = DimensionHandle.reconcile(leftAsset.dimension, rightAsset.dimension)
    contractAssert(
      dimensionResult.isRight,
      s"asset dimension $id did not reconcile across catalog generations: $dimensionResult"
    )

  private def assertGridReconciles(
    left: CatalogSnapshot,
    right: CatalogSnapshot,
    identity: GridIdentity
  ): Unit =
    val leftGrid   = grid(left, identity)
    val rightGrid  = grid(right, identity)
    val gridResult = GridHandle.reconcile(leftGrid, rightGrid)
    contractAssert(gridResult.isRight, s"grid $identity did not reconcile across catalog generations: $gridResult")
    val dimensionResult = DimensionHandle.reconcile(leftGrid.dimension, rightGrid.dimension)
    contractAssert(
      dimensionResult.isRight,
      s"grid dimension $identity did not reconcile across catalog generations: $dimensionResult"
    )

  private def assertAssetGridRelationship(
    snapshot: CatalogSnapshot,
    assetId: AssetId,
    gridIdentity: GridIdentity
  ): Unit =
    val assetValue = asset(snapshot, assetId)
    val gridValue  = grid(snapshot, gridIdentity)
    val result     = DimensionHandle.reconcile(assetValue.dimension, gridValue.dimension)
    contractAssert(result.isRight, s"asset/grid dimension relationship did not reconcile in one snapshot: $result")

  private def assertOutcomeEquivalent(
    actual: Either[CatalogViolations, CatalogCommit],
    expected: Either[CatalogViolations, CatalogCommit]
  ): Unit =
    (actual, expected) match
      case (Left(actualErrors), Left(expectedErrors)) =>
        contractAssertEquals(actualErrors, expectedErrors)
      case (Right(CatalogCommit.Unchanged(actualSnapshot)), Right(CatalogCommit.Unchanged(expectedSnapshot))) =>
        assertSnapshotShape(actualSnapshot, expectedSnapshot)
      case (
          Right(CatalogCommit.Published(actualSnapshot, actualDelta)),
          Right(CatalogCommit.Published(expectedSnapshot, expectedDelta))
        ) =>
        assertSnapshotShape(actualSnapshot, expectedSnapshot)
        contractAssertEquals(actualDelta, expectedDelta)
      case _ =>
        contractFail(s"live outcome $actual did not match pure outcome $expected")

  final def assertSequentialModelEquivalence(): F[Unit] =
    val initialAsset     = lineageSeedAsset
    val initialGrid      = lineageSeedGrid
    val initial          = required(CatalogModel.commit(CatalogRoot.create().initialState, lineageSeedBatch)).state
    val first            = definition("sequential-first")
    val second           = definition("sequential-second")
    val conflictingFirst = AssetDefinition(first.id, AtomId("contract:sequential-conflict"))
    val expectedFirst    = required(CatalogModel.commit(initial, batch(first)))
    val expectedSecond   = required(CatalogModel.commit(expectedFirst.state, batch(second)))
    val expectedConflict = pureOutcome(expectedSecond.state, conflictingFirst)
    val expectedRetry    = pureOutcome(expectedSecond.state, second)

    for
      catalog           <- requiredCatalog(Some(lineageSeedBatch))
      actualInitial     <- catalog.snapshot
      actualFirst       <- catalog.commit(batch(first))
      actualFirstCommit  = required(actualFirst)
      actualSecond      <- catalog.commit(batch(second))
      actualSecondCommit = required(actualSecond)
      actualConflict    <- catalog.commit(batch(conflictingFirst))
      actualRetry       <- catalog.commit(batch(second))
      retryCommit        = required(actualRetry)
      finalSnapshot     <- catalog.snapshot
    yield
      assertSnapshotShape(actualInitial, initial.snapshot)
      assertAssetGridRelationship(actualInitial, initialAsset.id, initialGrid.identity)

      assertOutcomeEquivalent(actualFirst, Right(expectedFirst.outcome))
      assertAsset(actualFirstCommit.snapshot, first)
      assertAssetReconciles(actualInitial, actualFirstCommit.snapshot, initialAsset.id)
      assertGridReconciles(actualInitial, actualFirstCommit.snapshot, initialGrid.identity)
      assertAssetGridRelationship(actualFirstCommit.snapshot, initialAsset.id, initialGrid.identity)

      assertOutcomeEquivalent(actualSecond, Right(expectedSecond.outcome))
      assertAsset(actualSecondCommit.snapshot, first)
      assertAsset(actualSecondCommit.snapshot, second)
      assertAssetReconciles(actualFirstCommit.snapshot, actualSecondCommit.snapshot, initialAsset.id)
      assertAssetReconciles(actualFirstCommit.snapshot, actualSecondCommit.snapshot, first.id)
      assertGridReconciles(actualFirstCommit.snapshot, actualSecondCommit.snapshot, initialGrid.identity)
      assertAssetGridRelationship(actualSecondCommit.snapshot, initialAsset.id, initialGrid.identity)

      contractAssert(actualConflict.isLeft, s"expected typed conflict, got $actualConflict")
      assertOutcomeEquivalent(actualConflict, expectedConflict)
      contractAssert(actualRetry.exists(_.isInstanceOf[CatalogCommit.Unchanged]), actualRetry)
      assertOutcomeEquivalent(actualRetry, expectedRetry)
      assertAssetReconciles(actualSecondCommit.snapshot, retryCommit.snapshot, first.id)
      assertGridReconciles(actualSecondCommit.snapshot, retryCommit.snapshot, initialGrid.identity)

      assertSnapshotShape(finalSnapshot, expectedSecond.state.snapshot)
      assertAsset(finalSnapshot, first)
      assertAsset(finalSnapshot, second)
      assertAssetReconciles(actualSecondCommit.snapshot, finalSnapshot, initialAsset.id)
      assertAssetReconciles(actualSecondCommit.snapshot, finalSnapshot, second.id)
      assertGridReconciles(actualSecondCommit.snapshot, finalSnapshot, initialGrid.identity)
    end for
  end assertSequentialModelEquivalence

  final def assertIndependentConcurrentCommits(): F[Unit] =
    val first  = definition("independent-first")
    val second = definition("independent-second")
    for
      catalog  <- requiredCatalog(None)
      results  <- concurrently(catalog.commit(batch(first)), catalog.commit(batch(second)))
      snapshot <- catalog.snapshot
    yield
      val published = Vector(results._1, results._2).map:
        case Right(value: CatalogCommit.Published) => value
        case other                                 => contractFail(s"expected two published outcomes, got $other")

      contractAssertEquals(published.map(_.snapshot.revision.value).sorted, Vector(BigInt(1), BigInt(2)))
      val (firstPublished, secondPublished) = published match
        case Vector(firstResult, secondResult) => firstResult -> secondResult
        case other                             => contractFail(s"expected exactly two published outcomes, got $other")
      contractAssert(firstPublished.delta.additions.contains(CatalogAddition.Asset(first.id)), firstPublished.delta)
      contractAssert(secondPublished.delta.additions.contains(CatalogAddition.Asset(second.id)), secondPublished.delta)
      contractAssertEquals(snapshot.revision.value, BigInt(2))
      assertAsset(snapshot, first)
      assertAsset(snapshot, second)
  end assertIndependentConcurrentCommits

  final def assertConflictingConcurrentRevalidation(): F[Unit] =
    val first   = definition("conflicting", "contract:first")
    val second  = AssetDefinition(first.id, AtomId("contract:second"))
    val initial = CatalogRoot.create().initialState
    for
      catalog  <- requiredCatalog(None)
      results  <- concurrently(catalog.commit(batch(first)), catalog.commit(batch(second)))
      snapshot <- catalog.snapshot
    yield
      def assertWinnerAndLoser(
        winner: AssetDefinition,
        loser: AssetDefinition,
        published: CatalogCommit.Published,
        losingResult: Either[CatalogViolations, CatalogCommit]
      ): Unit =
        val expectedWinner = required(CatalogModel.commit(initial, batch(winner)))
        val expectedLoser  = pureOutcome(expectedWinner.state, loser)
        assertOutcomeEquivalent(Right(published), Right(expectedWinner.outcome))
        contractAssert(
          losingResult.isLeft,
          s"expected the losing commit to return a typed conflict, got $losingResult"
        )
        assertOutcomeEquivalent(losingResult, expectedLoser)
        assertSnapshotShape(snapshot, expectedWinner.state.snapshot)
        assertAsset(snapshot, winner)

      (results._1, results._2) match
        case (Right(published: CatalogCommit.Published), losing @ Left(_)) =>
          assertWinnerAndLoser(first, second, published, losing)
        case (losing @ Left(_), Right(published: CatalogCommit.Published)) =>
          assertWinnerAndLoser(second, first, published, losing)
        case other =>
          contractFail(
            s"exactly one conflicting commit must publish and the loser must return its typed conflict: $other"
          )
    end for
  end assertConflictingConcurrentRevalidation

  final def assertSnapshotCommitLinearization(): F[Unit] =
    val definitionValue = definition("snapshot-linearization")
    val initial         = CatalogRoot.create().initialState
    val expected        = required(CatalogModel.commit(initial, batch(definitionValue)))
    for
      catalog       <- requiredCatalog(None)
      overlap       <- concurrently(catalog.snapshot, catalog.commit(batch(definitionValue)))
      finalSnapshot <- catalog.snapshot
    yield
      assertOutcomeEquivalent(overlap._2, Right(expected.outcome))
      overlap._1.revision.value match
        case revision if revision == initial.revision.value =>
          assertSnapshotShape(overlap._1, initial.snapshot)
          contractAssertEquals(overlap._1.resolveAsset(definitionValue.id), Left(UnknownAsset(definitionValue.id)))
        case revision if revision == expected.state.revision.value =>
          assertSnapshotShape(overlap._1, expected.state.snapshot)
          assertAsset(overlap._1, definitionValue)
        case revision =>
          contractFail(s"overlapping snapshot observed non-linearized revision $revision")

      assertSnapshotShape(finalSnapshot, expected.state.snapshot)
      assertAsset(finalSnapshot, definitionValue)
  end assertSnapshotCommitLinearization

  final def assertBootstrapAndOrderedErrors(): F[Unit] =
    val duplicateId = required(AssetId.from("contract-invalid"))
    val missingGrid = GridDefinition(
      GridIdentity(
        DimKey.atom(AtomId("contract:missing")),
        GridKey(required(GridId.from("contract-missing-grid")), required(GridVersion.from(1)))
      ),
      required(PositiveRational.exact(1, 100))
    )
    val invalid = CatalogBatch.of(
      CatalogCommand.RegisterAsset(AssetDefinition(duplicateId, AtomId("contract:invalid-first"))),
      CatalogCommand.RegisterAsset(AssetDefinition(duplicateId, AtomId("contract:invalid-second"))),
      CatalogCommand.RegisterGrid(missingGrid)
    )
    val expectedErrors = requiredLeft(CatalogModel.commit(CatalogRoot.create().initialState, invalid))

    for
      emptyCatalog  <- requiredCatalog(None)
      emptySnapshot <- emptyCatalog.snapshot
      invalidResult <- create(Some(invalid))
    yield
      assertSnapshotShape(emptySnapshot, CatalogRoot.create().initialState.snapshot)
      val actualErrors = requiredLeft(invalidResult)
      contractAssertEquals(actualErrors, expectedErrors)
      contractAssertEquals(actualErrors.violations.size, 2)
      val order = actualErrors.violations.map(value => value.commandIndex -> value.ruleOrdinal)
      contractAssertEquals(order, order.sorted)
  end assertBootstrapAndOrderedErrors

  final def assertIndependentLineages(): F[Unit] =
    val additional = definition("lineage-additional")
    for
      first        <- requiredCatalog(Some(lineageSeedBatch))
      second       <- requiredCatalog(Some(lineageSeedBatch))
      firstBefore  <- first.snapshot
      secondBefore <- second.snapshot
      firstCommit  <- first.commit(batch(additional))
      secondCommit <- second.commit(batch(additional))
      firstAfter   <- first.snapshot
      secondAfter  <- second.snapshot
    yield
      assertSnapshotShape(firstBefore, secondBefore)
      assertOutcomeEquivalent(firstCommit, secondCommit)
      assertSnapshotShape(firstAfter, secondAfter)
      assertAsset(firstAfter, lineageSeedAsset)
      assertAsset(secondAfter, lineageSeedAsset)
      assertAsset(firstAfter, additional)
      assertAsset(secondAfter, additional)

      val firstAsset  = asset(firstAfter, lineageSeedAsset.id)
      val secondAsset = asset(secondAfter, lineageSeedAsset.id)
      contractAssert(Asset.reconcile(firstAsset, secondAsset).isLeft, "independent assets reconciled across lineages")
      contractAssert(
        DimensionHandle.reconcile(firstAsset.dimension, secondAsset.dimension).isLeft,
        "independent asset dimensions reconciled across lineages"
      )
      contractAssert(
        GridHandle.reconcile(grid(firstAfter, lineageSeedGrid.identity),
          grid(secondAfter, lineageSeedGrid.identity)).isLeft,
        "independent grids reconciled across lineages"
      )
    end for
  end assertIndependentLineages
end LiveCatalogContract
