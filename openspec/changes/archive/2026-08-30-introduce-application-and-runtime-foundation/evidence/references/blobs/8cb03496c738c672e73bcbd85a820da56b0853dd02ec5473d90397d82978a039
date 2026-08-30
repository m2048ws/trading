package trading.runtime

import cats.effect.IO
import cats.syntax.all.*
import munit.CatsEffectSuite

import trading.application.LiveCatalog
import trading.application.LiveCatalogContract
import trading.reference.*

final class InMemoryLiveCatalogContractSuite extends CatsEffectSuite:
  private object contract extends LiveCatalogContract[IO]:
    protected def create(
      bootstrap: Option[CatalogBatch]
    ): IO[Either[CatalogViolations, LiveCatalog[IO]]] =
      InMemoryLiveCatalog.create[IO](bootstrap)

    protected def delay[A](body: => A): IO[A] =
      IO.delay(body)

    protected def bind[A, B](effect: IO[A])(next: A => IO[B]): IO[B] =
      effect.flatMap(next)

    protected def concurrently[A, B](left: IO[A], right: IO[B]): IO[(A, B)] =
      (left, right).parTupled
  end contract

  test("bootstrap and ordered failures agree with the pure model"):
    contract.assertBootstrapAndOrderedErrors()

  test("lookups, publications, conflicts, retries, revisions, deltas, and handles agree with the pure model"):
    contract.assertSequentialModelEquivalence()

  test("independent concurrent batches publish consecutive revisions without lost updates"):
    contract.assertIndependentConcurrentCommits()

  test("conflicting concurrent batches revalidate and return the pure typed conflict"):
    contract.assertConflictingConcurrentRevalidation()

  test("overlapping snapshot and commit expose one coherent linearized generation"):
    contract.assertSnapshotCommitLinearization()

  test("independent equal interpreters have equivalent outcomes and distinct lineages"):
    contract.assertIndependentLineages()
end InMemoryLiveCatalogContractSuite
