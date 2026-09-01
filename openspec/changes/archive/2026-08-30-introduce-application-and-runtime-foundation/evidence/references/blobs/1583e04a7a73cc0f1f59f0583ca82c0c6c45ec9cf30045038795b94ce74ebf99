package trading.runtime

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.Outcome
import cats.effect.testkit.TestControl
import cats.syntax.all.*
import munit.CatsEffectSuite

import trading.application.LiveCatalog
import trading.quantity.AtomId
import trading.reference.*

final class InMemoryLiveCatalogConcurrencySuite extends CatsEffectSuite:
  test("real-runtime independent commits publish one total order without lost additions"):
    val definitions = Vector.tabulate(32)(index => definition(s"independent-$index", s"independent:$index"))
    for
      catalog  <- requiredCatalog()
      results  <- definitions.parTraverse(value => catalog.commit(assetBatch(value)))
      snapshot <- catalog.snapshot
    yield
      val published = results.map(requiredPublished)
      assertEquals(published.map(_.snapshot.revision.value).sorted, 1.to(definitions.size).map(BigInt(_)).toVector)
      assertEquals(snapshot.revision.value, BigInt(definitions.size))
      assertEquals(snapshot.assetCount, definitions.size)
      assertEquals(snapshot.dimensionCount, definitions.size)
      definitions.foreach(value => assert(snapshot.resolveAsset(value.id).isRight, value.id.toString))

  test("real-runtime conflicting commits publish once and revalidate every loser"):
    val id          = required(AssetId.from("concurrent-conflict"))
    val definitions = Vector.tabulate(24)(index => AssetDefinition(id, AtomId(s"conflict:$index")))
    for
      catalog  <- requiredCatalog()
      results  <- definitions.parTraverse(value => catalog.commit(assetBatch(value)))
      snapshot <- catalog.snapshot
    yield
      val paired  = definitions.zip(results)
      val winners = paired.collect:
        case (value, Right(commit: CatalogCommit.Published)) => value -> commit
      val (winner, published) = winners match
        case Vector(single) => single
        case other          => fail(s"expected exactly one published commit, got $other")
      val pureWinner = required(CatalogModel.commit(CatalogRoot.create().initialState, assetBatch(winner)))
      val pureDelta  = requiredPublished(Right(pureWinner.outcome)).delta
      assertEquals(published.snapshot.revision.value, BigInt(1))
      assertEquals(snapshot.revision.value, BigInt(1))
      assertEquals(snapshot.assetCount, 1)

      paired.foreach:
        case (value, Right(commit: CatalogCommit.Published)) =>
          assertEquals(value, winner)
          assertEquals(commit.delta, pureDelta)
        case (value, result) =>
          val expected = requiredLeft(CatalogModel.commit(pureWinner.state, assetBatch(value)))
          assertEquals(result, Left(expected))
    end for

  test("TestControl proves cancellation exposes a predecessor or a complete retryable successor"):
    TestControl.executeEmbed:
      val batch = assetBatch(definition("cancelled-publication", "cancellation:publication"))
      for
        catalog       <- requiredCatalog()
        cancelled     <- (IO.canceled *> catalog.commit(batch)).start
        cancelledExit <- cancelled.join
        predecessor   <- catalog.snapshot
        acknowledged  <- Deferred[IO, CatalogCommit]
        publishing    <- catalog
                        .commit(batch)
                        .flatMap:
                          case Left(errors)  => IO.raiseError(new AssertionError(errors.toString))
                          case Right(commit) => acknowledged.complete(commit) *> IO.never
                        .start
        published <- acknowledged.get
        _         <- publishing.cancel
        successor <- catalog.snapshot
        retry     <- catalog.commit(batch)
      yield
        assert(cancelledExit.isInstanceOf[Outcome.Canceled[IO, Throwable, ?]], cancelledExit)
        assertEquals(predecessor.revision, CatalogRevision.zero)
        assertEquals(published.snapshot.revision.value, BigInt(1))
        assertEquals(successor.revision, published.snapshot.revision)
        val unchanged = requiredUnchanged(retry)
        assertEquals(unchanged.snapshot.revision, successor.revision)
      end for

  test("snapshot/commit stress stays coherent and separately created interpreters stay isolated"):
    val firstDefinitions  = Vector.tabulate(80)(index => definition(s"stress-first-$index", s"stress:first:$index"))
    val secondDefinitions = Vector.tabulate(80)(index => definition(s"stress-second-$index", s"stress:second:$index"))
    for
      first    <- requiredCatalog()
      second   <- requiredCatalog()
      firstRun <- (
                    firstDefinitions.parTraverse(value => first.commit(assetBatch(value))),
                    Vector.fill(240)(IO.cede *> first.snapshot).parSequence
                  ).parTupled
      secondResults <- secondDefinitions.parTraverse(value => second.commit(assetBatch(value)))
      firstFinal    <- first.snapshot
      secondFinal   <- second.snapshot
    yield
      firstRun._1.foreach(requiredPublished)
      secondResults.foreach(requiredPublished)
      firstRun._2.foreach: snapshot =>
        assertEquals(BigInt(snapshot.assetCount), snapshot.revision.value)
        assertEquals(snapshot.dimensionCount, snapshot.assetCount)
        assertEquals(snapshot.gridCount, 0)

      assertEquals(firstFinal.assetCount, firstDefinitions.size)
      assertEquals(secondFinal.assetCount, secondDefinitions.size)
      assert(firstFinal.resolveAsset(required(AssetId.from("stress-second-0"))).isLeft)
      assert(secondFinal.resolveAsset(required(AssetId.from("stress-first-0"))).isLeft)
    end for

  private def requiredCatalog(): IO[LiveCatalog[IO]] =
    InMemoryLiveCatalog.create[IO](None).map:
      case Left(errors)   => fail(s"expected a live catalog, got $errors")
      case Right(catalog) => catalog

  private def definition(id: String, atom: String): AssetDefinition =
    AssetDefinition(required(AssetId.from(id)), AtomId(atom))

  private def assetBatch(value: AssetDefinition): CatalogBatch =
    CatalogBatch.one(CatalogCommand.RegisterAsset(value))

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
end InMemoryLiveCatalogConcurrencySuite
