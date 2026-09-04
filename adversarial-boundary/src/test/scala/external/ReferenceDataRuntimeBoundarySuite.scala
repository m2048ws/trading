package external

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.reference.*

class ReferenceDataRuntimeBoundarySuite extends FunSuite:
  private val cent = PositiveRational.exact(1, 100).fold(error => fail(error.toString), identity)

  private def validAssetId(value: String): AssetId =
    AssetId.from(value).fold(error => fail(s"expected valid asset ID, got $error"), identity)

  private def validGridId(value: String): GridId =
    GridId.from(value).fold(error => fail(s"expected valid grid ID, got $error"), identity)

  private def validGridVersion(value: Long): GridVersion =
    GridVersion.from(value).fold(error => fail(s"expected valid grid version, got $error"), identity)

  private def assetDefinition(name: String): AssetDefinition =
    AssetDefinition(validAssetId(name), AtomId(s"runtime:$name"))

  private def gridDefinition(
    dimension: DimKey,
    name: String,
    quantum: PositiveRational = cent
  ): GridDefinition =
    GridDefinition(
      GridIdentity(dimension, GridKey(validGridId(name), validGridVersion(1))),
      quantum
    )

  private def transition(state: CatalogState, commands: CatalogCommand*): CatalogTransition =
    CatalogModel.commit(state, CatalogBatch.of(commands.head, commands.tail*)).fold(
      errors => fail(errors.toString),
      identity
    )

  private def catalog(assetName: String, grids: String*): CatalogTransition =
    val asset     = assetDefinition(assetName)
    val dimension = DimKey.atom(asset.dimensionAtom)
    transition(
      CatalogRoot.create().initialState,
      (CatalogCommand.RegisterAsset(asset) +:
        grids.toVector.map(name => CatalogCommand.RegisterGrid(gridDefinition(dimension, name))))*
    )

  private def rejectSerialization(value: AnyRef): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  private def rejectsNullAtRoot(body: => Any): Unit =
    var returned = false
    val _        = intercept[NullPointerException]:
      val _ = body
      returned = true
    assert(!returned)

  test("packaged stable identity smart constructors return precise typed failures"):
    assertEquals(AssetId.from("  "), Left(EmptyAssetId))
    assertEquals(GridId.from(""), Left(EmptyGridId))
    assertEquals(GridVersion.from(0), Left(NonPositiveGridVersion(0)))
    assertEquals(GridVersion.from(-7), Left(NonPositiveGridVersion(-7)))
    assertEquals(CatalogRevision.from(BigInt(-1)), Left(NegativeCatalogRevision(BigInt(-1))))
    assertEquals(CatalogBatch.from(Vector.empty), Left(EmptyCatalogBatch))

    assertEquals(validAssetId("equal"), validAssetId("equal"))
    assertEquals(validGridId("equal"), validGridId("equal"))
    assertEquals(validAssetId("observed").value, "observed")
    assertEquals(validGridId("observed-grid").value, "observed-grid")
    assertEquals(validGridVersion(3).value, 3L)
    assertEquals(validGridVersion(3).toString, "GridVersion(3)")
    val _ = intercept[UnsupportedOperationException](AssetId.fromProduct(Tuple1("unchecked")))
    val _ = intercept[UnsupportedOperationException](GridId.fromProduct(Tuple1("unchecked")))
    val _ = intercept[UnsupportedOperationException](GridVersion.fromProduct(Tuple1(1L)))

  test("packaged invariant-bearing products expose no product-copy bypass"):
    List(classOf[GridDefinition], classOf[CatalogBatch], classOf[CatalogDelta], classOf[CatalogRevision]).foreach:
      domainClass => assert(!classOf[Product].isAssignableFrom(domainClass))

  test("packaged catalog issues handles through pure transitions and direct snapshots"):
    val result       = catalog("private-jvm", "private-jvm-grid")
    val snapshot     = result.state.snapshot
    val definition   = assetDefinition("private-jvm")
    val asset        = snapshot.resolveAsset(definition.id).toOption.get
    val gridIdentity = gridDefinition(asset.dimension.key, "private-jvm-grid").identity
    val grid         = snapshot.resolveGrid(asset.dimension)(gridIdentity.key).toOption.get

    assert(snapshot.resolveAsset(asset.id).contains(asset))
    assert(snapshot.resolveGrid(grid.identity).contains(grid))
    assertEquals(snapshot.revision.value, BigInt(1))
    assertEquals(snapshot.assetCount, 1)
    assertEquals(snapshot.dimensionCount, 1)
    assertEquals(snapshot.gridCount, 1)

  test("packaged catalog values and trusted handles fail Java serialization"):
    val definition          = assetDefinition("serialization")
    val result              = catalog("serialization", "serialization-grid")
    val snapshot            = result.state.snapshot
    val asset               = snapshot.resolveAsset(definition.id).toOption.get
    val gridDefinitionValue = gridDefinition(asset.dimension.key, "serialization-grid")
    val grid                = snapshot.resolveGrid(gridDefinitionValue.identity).toOption.get

    List[AnyRef](
      definition.id,
      definition,
      gridDefinitionValue,
      CatalogCommand.RegisterAsset(definition),
      CatalogBatch.one(CatalogCommand.RegisterAsset(definition)),
      CatalogRevision.zero,
      result,
      result.state,
      snapshot,
      result.outcome,
      asset,
      asset.dimension,
      grid
    ).foreach(rejectSerialization)

  test("packaged append-only publication, history, and reconciliation preserve authority distinctions"):
    val localRoot     = CatalogRoot.create()
    val usdDefinition = assetDefinition("usd")
    val btcDefinition = assetDefinition("btc")
    val first         = transition(
      localRoot.initialState,
      CatalogCommand.RegisterAsset(usdDefinition),
      CatalogCommand.RegisterAsset(btcDefinition),
      CatalogCommand.RegisterGrid(gridDefinition(DimKey.atom(usdDefinition.dimensionAtom), "cent")),
      CatalogCommand.RegisterGrid(gridDefinition(DimKey.atom(usdDefinition.dimensionAtom), "other-cent"))
    )
    val oldSnapshot = first.state.snapshot
    val usd         = oldSnapshot.resolveAsset(usdDefinition.id).toOption.get
    val btc         = oldSnapshot.resolveAsset(btcDefinition.id).toOption.get
    val cents       = oldSnapshot.resolveGrid(gridDefinition(usd.dimension.key, "cent").identity).toOption.get
    val otherCents  = oldSnapshot.resolveGrid(gridDefinition(usd.dimension.key, "other-cent").identity).toOption.get

    val retry = CatalogModel.commit(
      first.state,
      CatalogBatch.one(CatalogCommand.RegisterAsset(usdDefinition))
    ).toOption.get
    assert(retry.state.eq(first.state))
    assert(retry.outcome.isInstanceOf[CatalogCommit.Unchanged])
    assert(Asset.reconcile(usd, retry.outcome.snapshot.resolveAsset(usd.id).toOption.get).isRight)
    assertEquals(Asset.reconcile(usd, btc), Left(AssetIdentityMismatch(usd.id, btc.id)))
    assertEquals(
      DimensionHandle.reconcile(usd.dimension, btc.dimension),
      Left(HandleDimensionMismatch(usd.dimension.key, btc.dimension.key))
    )
    assertEquals(
      GridHandle.reconcile(cents, otherCents),
      Left(StableGridIdentityMismatch(cents.identity, otherCents.identity))
    )
    assertEquals(SameGrid.between(cents.grid, otherCents.grid), Left(AnonymousGridMismatch))
    assert(SameQuantum.between(cents.grid, otherCents.grid).isRight)

    val laterDefinition = assetDefinition("later")
    val later           = transition(first.state, CatalogCommand.RegisterAsset(laterDefinition))
    assertEquals(oldSnapshot.resolveAsset(laterDefinition.id), Left(UnknownAsset(laterDefinition.id)))
    assert(later.state.snapshot.resolveAsset(laterDefinition.id).isRight)
    assert(Asset.reconcile(usd, later.state.snapshot.resolveAsset(usd.id).toOption.get).isRight)

    val foreign = transition(CatalogRoot.create().initialState, CatalogCommand.RegisterAsset(usdDefinition))
      .state.snapshot.resolveAsset(usd.id).toOption.get
    assertEquals(Asset.reconcile(usd, foreign), Left(ForeignLineage(usd.dimension.key, foreign.dimension.key)))

    val coordinate = cents.fromCoordinate(BigInt(37))
    assertEquals(cents.coordinate(coordinate), BigInt(37))
    assertEquals(cents.asQuantity(coordinate).coefficient, Rational(37, 100))

  test("packaged reference-data roots reject null before returning a value or authority"):
    rejectsNullAtRoot(AssetId.from(null))
    rejectsNullAtRoot(GridId.from(null))
    rejectsNullAtRoot(CatalogRevision.from(null))
    rejectsNullAtRoot(CatalogBatch.one(null))

    val result   = catalog("null-roots", "null-grid")
    val snapshot = result.state.snapshot
    val asset    = snapshot.resolveAsset(validAssetId("null-roots")).toOption.get
    val grid     = snapshot.resolveGrid(gridDefinition(asset.dimension.key, "null-grid").identity).toOption.get
    val nullDimension: DimensionHandle[asset.D] = null
    val nullAsset: Asset                        = null
    val nullGrid: GridHandle[asset.D]           = null
    val nullCoordinate: BigInt                  = null
    val nullRational: Rational                  = null

    rejectsNullAtRoot(CatalogModel.commit(null, CatalogBatch.one(CatalogCommand.RegisterAsset(assetDefinition("x")))))
    rejectsNullAtRoot(CatalogModel.commit(result.state, null))
    rejectsNullAtRoot(snapshot.resolveAsset(null))
    rejectsNullAtRoot(snapshot.resolveDimension(null))
    rejectsNullAtRoot(snapshot.resolveGrid(null))
    rejectsNullAtRoot(snapshot.resolveGrid(nullDimension)(grid.key))
    rejectsNullAtRoot(GridDefinition.from(grid.identity, nullRational))
    rejectsNullAtRoot(UniformGrid.from(asset.dimension.ref, nullRational))
    rejectsNullAtRoot(DimensionHandle.sameLineage(nullDimension, asset.dimension))
    rejectsNullAtRoot(Asset.reconcile(nullAsset, asset))
    rejectsNullAtRoot(GridHandle.reconcile(nullGrid, grid))
    rejectsNullAtRoot(grid.fromCoordinate(nullCoordinate))
end ReferenceDataRuntimeBoundarySuite
