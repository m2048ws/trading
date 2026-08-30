package trading.runtime

import cats.effect.IO
import cats.effect.Resource
import munit.CatsEffectSuite

import trading.application.LiveCatalog
import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.refinement.PositiveRational
import trading.reference.*

final class InMemoryLiveCatalogConstructionSuite extends CatsEffectSuite:
  test("empty construction allocates one fresh revision-zero catalog"):
    for
      catalog  <- requiredCatalog(None)
      snapshot <- catalog.snapshot
    yield
      assertEquals(snapshot.revision, CatalogRevision.zero)
      assertEquals(snapshot.assetCount, 0)
      assertEquals(snapshot.dimensionCount, 0)
      assertEquals(snapshot.gridCount, 0)

  test("valid bootstrap is evaluated before exposure and an identical retry is unchanged"):
    val bootstrap  = validBootstrap("construction-valid")
    val additional = assetBatch("construction-additional")
    for
      catalog     <- requiredCatalog(Some(bootstrap))
      initial     <- catalog.snapshot
      retry       <- catalog.commit(bootstrap)
      publication <- catalog.commit(additional)
      after       <- catalog.snapshot
    yield
      assertEquals(initial.revision.value, BigInt(1))
      assertEquals(initial.assetCount, 1)
      assertEquals(initial.dimensionCount, 1)
      assertEquals(initial.gridCount, 1)
      assert(retry.exists(_.isInstanceOf[CatalogCommit.Unchanged]), retry)
      val published = publication match
        case Right(value: CatalogCommit.Published) => value
        case other                                 => fail(s"expected a published addition, got $other")
      val id        = required(AssetId.from("construction-additional"))
      val dimension = DimKey.atom(AtomId("construction:construction-additional"))
      assertEquals(published.snapshot.revision.value, BigInt(2))
      assertEquals(
        published.delta.additions,
        Vector(CatalogAddition.Dimension(dimension), CatalogAddition.Asset(id))
      )
      assertEquals(after.revision, published.snapshot.revision)
      assertEquals(after.assetCount, initial.assetCount + 1)
      assertEquals(after.dimensionCount, initial.dimensionCount + 1)
      assertEquals(after.gridCount, initial.gridCount)
    end for

  test("invalid bootstrap returns the pure ordered violations and no interpreter"):
    val id      = required(AssetId.from("construction-invalid"))
    val invalid = CatalogBatch.of(
      CatalogCommand.RegisterAsset(AssetDefinition(id, AtomId("construction:first"))),
      CatalogCommand.RegisterAsset(AssetDefinition(id, AtomId("construction:second")))
    )
    val expected = CatalogModel.commit(CatalogRoot.create().initialState, invalid).swap.toOption.get

    InMemoryLiveCatalog.create[IO](Some(invalid)).map: result =>
      assertEquals(result, Left(expected))

  test("separate constructors with equal visible bootstrap establish distinct lineages"):
    val bootstrap = validBootstrap("construction-lineage")
    for
      first          <- requiredCatalog(Some(bootstrap))
      second         <- requiredCatalog(Some(bootstrap))
      firstSnapshot  <- first.snapshot
      secondSnapshot <- second.snapshot
    yield
      assertEquals(firstSnapshot.revision, secondSnapshot.revision)
      assertEquals(firstSnapshot.assetCount, secondSnapshot.assetCount)
      val id          = required(AssetId.from("construction-lineage"))
      val firstAsset  = firstSnapshot.resolveAsset(id).toOption.get
      val secondAsset = secondSnapshot.resolveAsset(id).toOption.get
      assert(Asset.reconcile(firstAsset, secondAsset).isLeft)

  test("the effectful constructor lifts into a larger Resource without a ceremonial finalizer"):
    Resource
      .eval(requiredCatalog(None))
      .use(_.snapshot)
      .map(snapshot => assertEquals(snapshot.revision, CatalogRevision.zero))

  private def requiredCatalog(bootstrap: Option[CatalogBatch]): IO[LiveCatalog[IO]] =
    InMemoryLiveCatalog.create[IO](bootstrap).map:
      case Left(errors)   => fail(s"expected a live catalog, got $errors")
      case Right(catalog) => catalog

  private def validBootstrap(name: String): CatalogBatch =
    val asset = AssetDefinition(required(AssetId.from(name)), AtomId(s"construction:$name"))
    val grid  = GridDefinition(
      GridIdentity(
        DimKey.atom(asset.dimensionAtom),
        GridKey(required(GridId.from(s"$name-grid")), required(GridVersion.from(1)))
      ),
      required(PositiveRational.exact(1, 100))
    )
    CatalogBatch.of(
      CatalogCommand.RegisterAsset(asset),
      CatalogCommand.RegisterGrid(grid)
    )

  private def assetBatch(name: String): CatalogBatch =
    CatalogBatch.one(
      CatalogCommand.RegisterAsset(
        AssetDefinition(required(AssetId.from(name)), AtomId(s"construction:$name"))
      )
    )

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => fail(error.toString), identity)
end InMemoryLiveCatalogConstructionSuite
