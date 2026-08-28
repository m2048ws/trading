package trading.application

import java.util.concurrent.CompletableFuture

import munit.FailException
import munit.FunSuite

import trading.quantity.AtomId
import trading.reference.*

private type Thunk[A] = () => A

private abstract class ThunkLiveCatalogContract extends LiveCatalogContract[Thunk]:
  protected final def run[A](effect: Thunk[A]): A = effect()

  protected final def concurrently[A, B](left: Thunk[A], right: Thunk[B]): (A, B) =
    val leftResult  = CompletableFuture.supplyAsync(() => left())
    val rightResult = CompletableFuture.supplyAsync(() => right())
    (leftResult.join(), rightResult.join())
end ThunkLiveCatalogContract

private class AtomicTestLiveCatalog(initial: CatalogState) extends LiveCatalog[Thunk]:
  protected var current: CatalogState = initial

  def snapshot: Thunk[CatalogSnapshot] =
    () => synchronized(current.snapshot)

  def commit(batch: CatalogBatch): Thunk[Either[CatalogViolations, CatalogCommit]] =
    () =>
      synchronized:
        CatalogModel.commit(current, batch).map: transition =>
          current = transition.state
          transition.outcome
end AtomicTestLiveCatalog

private final class ConflictMaskingLiveCatalog(initial: CatalogState) extends AtomicTestLiveCatalog(initial):
  private var lastSuccessfulBatch: Option[CatalogBatch] = None

  override def commit(batch: CatalogBatch): Thunk[Either[CatalogViolations, CatalogCommit]] =
    () =>
      synchronized:
        CatalogModel.commit(current, batch) match
          case Left(_) =>
            lastSuccessfulBatch match
              case Some(successful) => CatalogModel.commit(current, successful).map(_.outcome)
              case None             => throw new AssertionError("conflict masking requires a prior successful batch")
          case Right(transition) =>
            lastSuccessfulBatch = Some(batch)
            current = transition.state
            Right(transition.outcome)
end ConflictMaskingLiveCatalog

private final class TornSnapshotLiveCatalog(initial: CatalogState) extends AtomicTestLiveCatalog(initial):
  private val decoySnapshot =
    val definition = AssetDefinition(AssetId.from("linearization-decoy").toOption.get, AtomId("contract:decoy"))
    CatalogModel
      .commit(
        CatalogRoot.create().initialState,
        CatalogBatch.one(CatalogCommand.RegisterAsset(definition))
      )
      .toOption
      .get
      .state
      .snapshot
  private var tearNextSnapshot = true

  override def snapshot: Thunk[CatalogSnapshot] =
    () =>
      synchronized:
        if tearNextSnapshot then
          tearNextSnapshot = false
          decoySnapshot
        else current.snapshot
end TornSnapshotLiveCatalog

private final class LineageResettingLiveCatalog(
  initial: CatalogState,
  initialBatches: Vector[CatalogBatch])
  extends AtomicTestLiveCatalog(initial):
  private var successfulBatches = initialBatches

  override def commit(batch: CatalogBatch): Thunk[Either[CatalogViolations, CatalogCommit]] =
    () =>
      synchronized:
        val rebuilt = successfulBatches.foldLeft(CatalogRoot.create().initialState): (state, successful) =>
          CatalogModel
            .commit(state, successful)
            .fold(errors => throw new AssertionError(s"successful replay failed: $errors"), _.state)

        CatalogModel.commit(rebuilt, batch).map: transition =>
          transition.outcome match
            case _: CatalogCommit.Published => successfulBatches = successfulBatches :+ batch
            case _: CatalogCommit.Unchanged => ()
          current = transition.state
          transition.outcome
end LineageResettingLiveCatalog

final class LiveCatalogContractSuite extends ThunkLiveCatalogContract:
  protected def live(initial: CatalogState): LiveCatalog[Thunk] =
    new AtomicTestLiveCatalog(initial)
end LiveCatalogContractSuite

final class LiveCatalogContractRejectionSuite extends FunSuite:
  test("the reusable contract rejects an interpreter that hides pure typed conflicts"):
    val broken = new ThunkLiveCatalogContract:
      protected def live(initial: CatalogState): LiveCatalog[Thunk] =
        new ConflictMaskingLiveCatalog(initial)

    val failure = intercept[FailException](broken.assertSequentialModelEquivalence())
    assert(failure.getMessage.contains("expected typed conflict"), failure.getMessage)

  test("the reusable contract rejects a non-linearized overlapping snapshot"):
    val broken = new ThunkLiveCatalogContract:
      protected def live(initial: CatalogState): LiveCatalog[Thunk] =
        new TornSnapshotLiveCatalog(initial)

    val failure = intercept[FailException](broken.assertSnapshotCommitLinearization())
    assert(failure.getMessage.contains("UnknownAsset"), failure.getMessage)

  test("the reusable contract rejects an interpreter that replaces catalog lineage"):
    val broken = new ThunkLiveCatalogContract:
      protected def live(initial: CatalogState): LiveCatalog[Thunk] =
        new LineageResettingLiveCatalog(initial, Vector(lineageSeedBatch))

    val failure = intercept[FailException](broken.assertSequentialModelEquivalence())
    assert(failure.getMessage.contains("reconcile"), failure.getMessage)
end LiveCatalogContractRejectionSuite
