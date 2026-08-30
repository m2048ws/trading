package trading.application

import java.util.concurrent.CompletableFuture

import munit.FunSuite

import trading.quantity.AtomId
import trading.reference.*

private type Thunk[A] = () => A

private def required[E, A](value: Either[E, A]): A =
  value.fold(error => throw new AssertionError(error.toString), identity)

private abstract class ThunkLiveCatalogContract extends LiveCatalogContract[Thunk]:
  protected def newCatalog(initial: CatalogState, successfulBatches: Vector[CatalogBatch]): LiveCatalog[Thunk]

  protected final def create(
    bootstrap: Option[CatalogBatch]
  ): Thunk[Either[CatalogViolations, LiveCatalog[Thunk]]] =
    () =>
      val empty = CatalogRoot.create().initialState
      bootstrap match
        case None        => Right(newCatalog(empty, Vector.empty))
        case Some(batch) =>
          CatalogModel.commit(empty, batch).map: transition =>
            newCatalog(transition.state, Vector(batch))

  protected final def delay[A](body: => A): Thunk[A] =
    () => body

  protected final def bind[A, B](effect: Thunk[A])(next: A => Thunk[B]): Thunk[B] =
    () => next(effect())()

  protected final def concurrently[A, B](left: Thunk[A], right: Thunk[B]): Thunk[(A, B)] =
    () =>
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
    val definition = AssetDefinition(required(AssetId.from("linearization-decoy")), AtomId("contract:decoy"))
    required(
      CatalogModel.commit(
        CatalogRoot.create().initialState,
        CatalogBatch.one(CatalogCommand.RegisterAsset(definition))
      )
    ).state.snapshot
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

private object ThunkCatalogContract extends ThunkLiveCatalogContract:
  protected def newCatalog(initial: CatalogState, successfulBatches: Vector[CatalogBatch]): LiveCatalog[Thunk] =
    new AtomicTestLiveCatalog(initial)
end ThunkCatalogContract

final class LiveCatalogContractSuite extends FunSuite:
  test("bootstrap, ordered failures, commits, conflicts, and retries agree with the pure model"):
    ThunkCatalogContract.assertBootstrapAndOrderedErrors()()
    ThunkCatalogContract.assertSequentialModelEquivalence()()

  test("independent concurrent batches publish consecutive revisions without lost updates"):
    ThunkCatalogContract.assertIndependentConcurrentCommits()()

  test("conflicting concurrent batches revalidate and return the pure typed conflict"):
    ThunkCatalogContract.assertConflictingConcurrentRevalidation()()

  test("overlapping snapshot and commit expose one coherent linearized generation"):
    ThunkCatalogContract.assertSnapshotCommitLinearization()()

  test("independent equal interpreters have equivalent outcomes and distinct lineages"):
    ThunkCatalogContract.assertIndependentLineages()()
end LiveCatalogContractSuite

final class LiveCatalogContractRejectionSuite extends FunSuite:
  test("the reusable contract rejects an interpreter that hides pure typed conflicts"):
    val broken = new ThunkLiveCatalogContract:
      protected def newCatalog(initial: CatalogState, successfulBatches: Vector[CatalogBatch]): LiveCatalog[Thunk] =
        new ConflictMaskingLiveCatalog(initial)

    val failure = intercept[AssertionError](broken.assertSequentialModelEquivalence()())
    assert(failure.getMessage.contains("expected typed conflict"), failure.getMessage)

  test("the reusable contract rejects a non-linearized overlapping snapshot"):
    val broken = new ThunkLiveCatalogContract:
      protected def newCatalog(initial: CatalogState, successfulBatches: Vector[CatalogBatch]): LiveCatalog[Thunk] =
        new TornSnapshotLiveCatalog(initial)

    val failure = intercept[AssertionError](broken.assertSnapshotCommitLinearization()())
    assert(failure.getMessage.contains("UnknownAsset"), failure.getMessage)

  test("the reusable contract rejects an interpreter that replaces catalog lineage"):
    val broken = new ThunkLiveCatalogContract:
      protected def newCatalog(initial: CatalogState, successfulBatches: Vector[CatalogBatch]): LiveCatalog[Thunk] =
        new LineageResettingLiveCatalog(initial, successfulBatches)

    val failure = intercept[AssertionError](broken.assertSequentialModelEquivalence()())
    assert(failure.getMessage.contains("reconcile"), failure.getMessage)
end LiveCatalogContractRejectionSuite
