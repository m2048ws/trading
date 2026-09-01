package trading.runtime

import cats.effect.IO
import munit.CatsEffectSuite

import trading.application.LiveCatalog
import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

final class InMemoryLiveCatalogAtomicSuite extends CatsEffectSuite:
  test("sequential publication conserves revisions and deltas across unchanged and failed commits"):
    val firstBatch  = assetBatch("atomic-first")
    val secondBatch = assetBatch("atomic-second")
    val conflict    = CatalogBatch.one(
      CatalogCommand.RegisterAsset(
        AssetDefinition(required(AssetId.from("atomic-first")), AtomId("atomic:conflict"))
      )
    )
    val accumulated = accumulatedInvalidBatch()

    val pureInitial      = CatalogRoot.create().initialState
    val pureFirst        = required(CatalogModel.commit(pureInitial, firstBatch))
    val expectedConflict = requiredLeft(CatalogModel.commit(pureFirst.state, conflict))
    val expectedErrors   = requiredLeft(CatalogModel.commit(pureFirst.state, accumulated))
    val pureSecond       = required(CatalogModel.commit(pureFirst.state, secondBatch))

    for
      catalog        <- requiredCatalog(None)
      initial        <- catalog.snapshot
      first          <- catalog.commit(firstBatch)
      retry          <- catalog.commit(firstBatch)
      conflictResult <- catalog.commit(conflict)
      errorResult    <- catalog.commit(accumulated)
      afterFailures  <- catalog.snapshot
      second         <- catalog.commit(secondBatch)
      finalSnapshot  <- catalog.snapshot
    yield
      val firstPublished  = requiredPublished(first)
      val retryUnchanged  = requiredUnchanged(retry)
      val secondPublished = requiredPublished(second)

      assertEquals(initial.revision, CatalogRevision.zero)
      assertEquals(firstPublished.snapshot.revision, pureFirst.state.revision)
      assertEquals(firstPublished.delta, requiredPublished(Right(pureFirst.outcome)).delta)
      assertEquals(retryUnchanged.snapshot.revision, firstPublished.snapshot.revision)
      assertEquals(conflictResult, Left(expectedConflict))
      assertEquals(errorResult, Left(expectedErrors))
      assertEquals(expectedErrors.violations.size, 2)
      assertEquals(
        expectedErrors.violations.map(value => (value.commandIndex, value.ruleOrdinal)),
        expectedErrors.violations.map(value => (value.commandIndex, value.ruleOrdinal)).sorted
      )
      assertEquals(afterFailures.revision, firstPublished.snapshot.revision)
      assertEquals(afterFailures.assetCount, firstPublished.snapshot.assetCount)
      assertEquals(afterFailures.dimensionCount, firstPublished.snapshot.dimensionCount)
      assertEquals(secondPublished.snapshot.revision, pureSecond.state.revision)
      assertEquals(secondPublished.delta, requiredPublished(Right(pureSecond.outcome)).delta)
      assertEquals(finalSnapshot.revision.value, BigInt(2))
      assertEquals(finalSnapshot.assetCount, 2)
      assertEquals(finalSnapshot.dimensionCount, 2)
      assertEquals(finalSnapshot.gridCount, 0)

      val publishedAdditions = firstPublished.delta.additions ++ secondPublished.delta.additions
      assertEquals(
        publishedAdditions,
        Vector(
          CatalogAddition.Dimension(DimKey.atom(AtomId("atomic:atomic-first"))),
          CatalogAddition.Asset(required(AssetId.from("atomic-first"))),
          CatalogAddition.Dimension(DimKey.atom(AtomId("atomic:atomic-second"))),
          CatalogAddition.Asset(required(AssetId.from("atomic-second")))
        )
      )
    end for

  test("captured snapshots remain immutable and retain canonical handles within one lineage"):
    val bootstrap      = assetAndGridBatch("snapshot-stable", 1)
    val additionalGrid = gridBatch("snapshot-stable", 2)
    val assetId        = required(AssetId.from("snapshot-stable"))
    val firstIdentity  = gridIdentity("snapshot-stable", 1)
    val secondIdentity = gridIdentity("snapshot-stable", 2)

    for
      catalog     <- requiredCatalog(Some(bootstrap))
      before      <- catalog.snapshot
      publication <- catalog.commit(additionalGrid)
      after       <- catalog.snapshot
      retry       <- catalog.commit(additionalGrid)
      afterRetry  <- catalog.snapshot
    yield
      val published = requiredPublished(publication)
      val unchanged = requiredUnchanged(retry)

      assertEquals(before.revision.value, BigInt(1))
      assertEquals(before.resolveGrid(secondIdentity), Left(UnknownGrid(secondIdentity)))
      assertEquals(published.snapshot.revision.value, BigInt(2))
      assertEquals(published.delta.additions, Vector(CatalogAddition.Grid(secondIdentity)))
      assert(after.resolveGrid(secondIdentity).isRight)
      assertEquals(before.resolveGrid(secondIdentity), Left(UnknownGrid(secondIdentity)))
      assertEquals(unchanged.snapshot.revision, after.revision)
      assertEquals(afterRetry.revision, after.revision)

      val beforeAsset     = required(before.resolveAsset(assetId))
      val afterAsset      = required(after.resolveAsset(assetId))
      val beforeFirstGrid = required(before.resolveGrid(firstIdentity))
      val afterFirstGrid  = required(after.resolveGrid(firstIdentity))
      val retryFirstGrid  = required(afterRetry.resolveGrid(firstIdentity))

      assert(Asset.reconcile(beforeAsset, afterAsset).isRight)
      assert(DimensionHandle.reconcile(beforeAsset.dimension, afterAsset.dimension).isRight)
      assert(GridHandle.reconcile(beforeFirstGrid, afterFirstGrid).isRight)
      assert(GridHandle.reconcile(afterFirstGrid, retryFirstGrid).isRight)
      assert(DimensionHandle.reconcile(afterAsset.dimension, afterFirstGrid.dimension).isRight)
    end for

  private def requiredCatalog(bootstrap: Option[CatalogBatch]): IO[LiveCatalog[IO]] =
    InMemoryLiveCatalog.create[IO](bootstrap).map:
      case Left(errors)   => fail(s"expected a live catalog, got $errors")
      case Right(catalog) => catalog

  private def assetBatch(name: String): CatalogBatch =
    CatalogBatch.one(
      CatalogCommand.RegisterAsset(
        AssetDefinition(required(AssetId.from(name)), AtomId(s"atomic:$name"))
      )
    )

  private def assetAndGridBatch(name: String, version: Int): CatalogBatch =
    val asset = AssetDefinition(required(AssetId.from(name)), AtomId(s"atomic:$name"))
    CatalogBatch.of(
      CatalogCommand.RegisterAsset(asset),
      CatalogCommand.RegisterGrid(gridDefinition(name, version))
    )

  private def gridBatch(name: String, version: Int): CatalogBatch =
    CatalogBatch.one(CatalogCommand.RegisterGrid(gridDefinition(name, version)))

  private def gridDefinition(name: String, version: Int): GridDefinition =
    GridDefinition(
      gridIdentity(name, version),
      required(PositiveRational.exact(1, BigInt(100) * version))
    )

  private def gridIdentity(name: String, version: Int): GridIdentity =
    GridIdentity(
      DimKey.atom(AtomId(s"atomic:$name")),
      GridKey(required(GridId.from(s"$name-grid")), required(GridVersion.from(version)))
    )

  private def accumulatedInvalidBatch(): CatalogBatch =
    val missingDimension = DimKey.atom(AtomId("atomic:missing"))
    val missingGrid      = GridDefinition(
      GridIdentity(
        missingDimension,
        GridKey(required(GridId.from("atomic-missing-grid")), required(GridVersion.from(1)))
      ),
      required(PositiveRational.exact(1, 100))
    )
    CatalogBatch.of(
      CatalogCommand.RegisterAsset(
        AssetDefinition(required(AssetId.from("atomic-first")), AtomId("atomic:conflict-two"))
      ),
      CatalogCommand.RegisterGrid(missingGrid)
    )

  private def requiredPublished(
    result: Either[CatalogViolations, CatalogCommit]
  ): CatalogCommit.Published =
    result match
      case Right(value: CatalogCommit.Published) => value
      case other                                 => fail(s"expected published commit, got $other")

  private def requiredUnchanged(
    result: Either[CatalogViolations, CatalogCommit]
  ): CatalogCommit.Unchanged =
    result match
      case Right(value: CatalogCommit.Unchanged) => value
      case other                                 => fail(s"expected unchanged commit, got $other")

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => fail(error.toString), identity)

  private def requiredLeft[E, A](value: Either[E, A]): E =
    value.fold(identity, success => fail(s"expected a typed failure, got $success"))
end InMemoryLiveCatalogAtomicSuite
