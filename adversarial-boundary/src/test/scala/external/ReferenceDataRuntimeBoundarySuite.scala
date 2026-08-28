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
    val emptyAsset: Either[EmptyAssetId.type, AssetId]               = AssetId.from("  ")
    val emptyGrid: Either[EmptyGridId.type, GridId]                  = GridId.from("")
    val zeroVersion: Either[NonPositiveGridVersion, GridVersion]     = GridVersion.from(0)
    val negativeVersion: Either[NonPositiveGridVersion, GridVersion] = GridVersion.from(-7)

    assertEquals(emptyAsset, Left(EmptyAssetId))
    assertEquals(emptyGrid, Left(EmptyGridId))
    assertEquals(zeroVersion, Left(NonPositiveGridVersion(0)))
    assertEquals(negativeVersion, Left(NonPositiveGridVersion(-7)))

    val firstAssetId  = validAssetId("equal")
    val secondAssetId = validAssetId("equal")
    val firstGridId   = validGridId("equal")
    val secondGridId  = validGridId("equal")
    val firstVersion  = validGridVersion(3)
    val secondVersion = validGridVersion(3)

    assertEquals(firstAssetId, secondAssetId)
    assertEquals(firstAssetId.hashCode, secondAssetId.hashCode)
    assertEquals(firstAssetId.toString, "AssetId(equal)")
    assertEquals(firstGridId, secondGridId)
    assertEquals(firstGridId.hashCode, secondGridId.hashCode)
    assertEquals(firstGridId.toString, "GridId(equal)")
    assertEquals(firstVersion, secondVersion)
    assertEquals(firstVersion.hashCode, secondVersion.hashCode)
    assertEquals(firstVersion.toString, "GridVersion(3)")

  test("packaged stable identity classes expose no case-class apply, copy, or product bypass"):
    List(classOf[AssetId], classOf[GridId], classOf[GridVersion]).foreach: identityClass =>
      assert(!classOf[Product].isAssignableFrom(identityClass))
      val forbidden = identityClass.getMethods.map(_.getName).toSet.intersect(Set("apply", "copy", "fromProduct"))
      assertEquals(forbidden, Set.empty[String])

    List(classOf[AssetId], classOf[GridId], classOf[GridVersion]).foreach: identityClass =>
      assert(
        identityClass.getDeclaredConstructors.forall(constructor =>
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers)
        )
      )

    assert(!classOf[Product].isAssignableFrom(classOf[GridDefinition]))
    val gridDefinitionForbidden =
      classOf[GridDefinition].getMethods.map(_.getName).toSet.intersect(Set("copy", "fromProduct"))
    assertEquals(gridDefinitionForbidden, Set.empty[String])

  test("packaged raw grid boundaries reject nonpositive quanta and retain nearby positive construction"):
    val gridId        = validGridId("checked-quantum")
    val version       = validGridVersion(1)
    val identityValue = GridIdentity(DimKey.one, GridKey(gridId, version))

    assertEquals(GridDefinition.from(identityValue, Rational.zero), Left(NonPositiveGridQuantum(Rational.zero)))
    assertEquals(
      GridDefinition.from(identityValue, -Rational.one),
      Left(NonPositiveGridQuantum(-Rational.one))
    )
    assertEquals(UniformGrid.from(DimRef.one, Rational.zero), Left(ExpectedPositive))
    assertEquals(UniformGrid.from(DimRef.one, -Rational.one), Left(ExpectedPositive))

    val positiveDefinition =
      GridDefinition.from(identityValue, Rational.one).fold(error => fail(error.toString), identity)
    val positiveGrid = UniformGrid.from(DimRef.one, Rational.one).fold(error => fail(error.toString), identity)
    val registry     = new QuantityRegistry
    val dimension    = registry
      .registerDimension(DimKey.one)
      .fold(error => fail(error.toString), identity)
    val positiveHandle = registry
      .registerGrid(dimension)(positiveDefinition)
      .fold(error => fail(error.toString), identity)

    assertEquals(positiveDefinition.quantum.unrefined, Rational.one)
    assertEquals(positiveGrid.quantum.unrefined, Rational.one)
    assertEquals(positiveHandle.quantum.unrefined, Rational.one)

  test("packaged registry handles are final value classes without implementation hierarchies"):
    val registry = new QuantityRegistry
    val asset    = registry.registerAsset(assetDefinition("private-jvm")).fold(error => fail(error.toString), identity)
    val grid     = registry
      .registerGrid(asset)(gridDefinition(asset.dimension.key, "private-jvm-grid"))
      .fold(error => fail(error.toString), identity)

    List(asset.dimension, asset, grid).foreach: handle =>
      val modifiers = handle.getClass.getModifiers
      assert(java.lang.reflect.Modifier.isFinal(modifiers))
      assert(!java.lang.reflect.Modifier.isAbstract(modifiers))
      assert(!handle.getClass.isInterface)

  test("packaged stable identities, definitions, errors, and handles fail Java serialization"):
    val registry             = new QuantityRegistry
    val assetDefinitionValue = assetDefinition("serialization")
    val asset               = registry.registerAsset(assetDefinitionValue).fold(error => fail(error.toString), identity)
    val gridDefinitionValue = gridDefinition(asset.dimension.key, "serialization-cent")
    val grid = registry.registerGrid(asset)(gridDefinitionValue).fold(error => fail(error.toString), identity)

    List[AnyRef](
      assetDefinitionValue.id,
      gridDefinitionValue.id,
      gridDefinitionValue.version,
      gridDefinitionValue.key,
      gridDefinitionValue.identity,
      assetDefinitionValue,
      gridDefinitionValue,
      EmptyAssetId,
      NonPositiveGridVersion(0),
      NonPositiveGridQuantum(Rational.zero),
      asset,
      asset.dimension,
      grid
    ).foreach(rejectSerialization)

  test("packaged canonical issuance and reconciliation preserve every authority distinction"):
    val local   = new QuantityRegistry
    val foreign = new QuantityRegistry

    val usdDefinition = assetDefinition("usd")
    val btcDefinition = assetDefinition("btc")
    val usd           = local.registerAsset(usdDefinition).fold(error => fail(error.toString), identity)
    val sameUsd       = local.registerAsset(usdDefinition).fold(error => fail(error.toString), identity)
    val btc           = local.registerAsset(btcDefinition).fold(error => fail(error.toString), identity)
    val foreignUsd    = foreign.registerAsset(usdDefinition).fold(error => fail(error.toString), identity)

    assert(usd.eq(sameUsd))
    assert(local.resolveAsset(usd.id).contains(usd))
    assert(Asset.reconcile(usd, sameUsd).isRight)
    assertEquals(Asset.reconcile(usd, btc), Left(AssetIdentityMismatch(usd.id, btc.id)))
    assertEquals(
      Asset.reconcile(usd, foreignUsd),
      Left(ForeignLineage(usd.dimension.key, foreignUsd.dimension.key))
    )
    assertEquals(
      DimensionHandle.reconcile(usd.dimension, btc.dimension),
      Left(HandleDimensionMismatch(usd.dimension.key, btc.dimension.key))
    )

    val centsDefinition = gridDefinition(usd.dimension.key, "cent")
    val cents           = local.registerGrid(usd)(centsDefinition).fold(error => fail(error.toString), identity)
    val sameCents       = local.registerGrid(usd)(centsDefinition).fold(error => fail(error.toString), identity)
    val otherCents      = local
      .registerGrid(usd)(gridDefinition(usd.dimension.key, "other-cent"))
      .fold(error => fail(error.toString), identity)
    val foreignCents = foreign
      .registerGrid(foreignUsd)(gridDefinition(foreignUsd.dimension.key, "cent"))
      .fold(error => fail(error.toString), identity)

    assert(cents.eq(sameCents))
    assert(local.resolveGrid(usd.dimension)(cents.key).contains(cents))
    assert(GridHandle.reconcile(cents, sameCents).isRight)
    assertEquals(
      GridHandle.reconcile(cents, otherCents),
      Left(StableGridIdentityMismatch(cents.identity, otherCents.identity))
    )
    assertEquals(
      GridHandle.reconcile(cents, foreignCents),
      Left(ForeignLineage(cents.dimension.key, foreignCents.dimension.key))
    )
    assertEquals(SameGrid.between(cents.grid, otherCents.grid), Left(AnonymousGridMismatch))
    assert(SameQuantum.between(cents.grid, otherCents.grid).isRight)

    val wrongDimension = gridDefinition(btc.dimension.key, "wrong-dimension")
    assertEquals(
      local.registerGrid(usd)(wrongDimension),
      Left(GridDefinitionDimensionMismatch(usd.dimension.key, btc.dimension.key))
    )

    val conflicting = gridDefinition(
      usd.dimension.key,
      "cent",
      PositiveRational.exact(3, 100).fold(error => fail(error.toString), identity)
    )
    assertEquals(
      local.registerGrid(usd)(conflicting),
      Left(
        ConflictingGridDefinition(
          centsDefinition.identity,
          centsDefinition.quantum.unrefined,
          conflicting.quantum.unrefined
        )
      )
    )

    val coordinate = cents.fromCoordinate(BigInt(37))
    assertEquals(cents.coordinate(coordinate), BigInt(37))
    assertEquals(cents.asQuantity(coordinate).coefficient, Rational(37, 100))

  test("packaged reference-data roots reject null before returning a value or authority"):
    rejectsNullAtRoot(AssetId.from(null))
    rejectsNullAtRoot(GridId.from(null))

    val version = validGridVersion(1)
    val id      = validGridId("null-grid")
    rejectsNullAtRoot(GridKey(null, version))
    rejectsNullAtRoot(GridKey(id, null))
    rejectsNullAtRoot(GridIdentity(null, GridKey(id, version)))
    rejectsNullAtRoot(GridIdentity(DimKey.one, null))
    rejectsNullAtRoot(AssetDefinition(null, AtomId("null-asset")))
    rejectsNullAtRoot(AssetDefinition(validAssetId("null-asset"), null))
    rejectsNullAtRoot(GridDefinition(null, cent))

    val registry   = new QuantityRegistry
    val asset      = registry.registerAsset(assetDefinition("null-roots")).fold(error => fail(error.toString), identity)
    val definition = gridDefinition(asset.dimension.key, "null-roots-grid")
    val grid       = registry.registerGrid(asset)(definition).fold(error => fail(error.toString), identity)
    val nullDimension: DimensionHandle[asset.D] = null
    val nullAsset: Asset                        = null
    val nullGrid: GridHandle[asset.D]           = null
    val nullCoordinate: BigInt                  = null
    val nullRational: Rational                  = null

    rejectsNullAtRoot(registry.registerAsset(null))
    rejectsNullAtRoot(registry.resolveAsset(null))
    rejectsNullAtRoot(registry.registerDimension(null))
    rejectsNullAtRoot(registry.resolveDimension(null))
    rejectsNullAtRoot(registry.registerGrid(nullDimension)(definition))
    rejectsNullAtRoot(registry.registerGrid(nullAsset)(definition))
    rejectsNullAtRoot(registry.registerGrid(asset)(null))
    rejectsNullAtRoot(registry.resolveGrid(nullDimension)(grid.key))
    rejectsNullAtRoot(registry.resolveGrid(asset.dimension)(null))
    rejectsNullAtRoot(GridDefinition.from(definition.identity, nullRational))
    rejectsNullAtRoot(UniformGrid.from(asset.dimension.ref, nullRational))
    rejectsNullAtRoot(DimensionHandle.sameLineage(nullDimension, asset.dimension))
    rejectsNullAtRoot(DimensionHandle.reconcile(asset.dimension, nullDimension))
    rejectsNullAtRoot(Asset.reconcile(nullAsset, asset))
    rejectsNullAtRoot(GridHandle.reconcile(nullGrid, grid))
    rejectsNullAtRoot(grid.fromCoordinate(nullCoordinate))

end ReferenceDataRuntimeBoundarySuite
