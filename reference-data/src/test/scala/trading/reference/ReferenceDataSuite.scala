package trading.reference

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

class ReferenceDataSuite extends FunSuite:

  private val cent = PositiveRational.exact(1, 100).toOption.get

  private def validAssetId(value: String): AssetId =
    AssetId.from(value).fold(error => fail(s"expected valid asset ID, got $error"), identity)

  private def validGridId(value: String): GridId =
    GridId.from(value).fold(error => fail(s"expected valid grid ID, got $error"), identity)

  private def validGridVersion(value: Long): GridVersion =
    GridVersion.from(value).fold(error => fail(s"expected valid grid version, got $error"), identity)

  private def assetDefinition(name: String): AssetDefinition =
    AssetDefinition(validAssetId(name), AtomId(s"asset:$name"))

  private def gridDefinition(dimension: DimKey, name: String, quantum: PositiveRational = cent): GridDefinition =
    GridDefinition(
      GridIdentity(dimension, GridKey(validGridId(name), validGridVersion(1))),
      quantum
    )

  private def rejectSerialization(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("stable identifiers are validated and full grid identity is dimension scoped"):
    val _ = intercept[NullPointerException](AssetId.from(null))
    assertEquals(AssetId.from("  "), Left(EmptyAssetId))
    val _ = intercept[NullPointerException](GridId.from(null))
    assertEquals(GridId.from(""), Left(EmptyGridId))
    assertEquals(GridVersion.from(0), Left(NonPositiveGridVersion(0)))
    assertEquals(GridVersion.from(-1), Left(NonPositiveGridVersion(-1)))

    val firstAssetId  = validAssetId("USD")
    val secondAssetId = validAssetId("USD")
    assertEquals(firstAssetId, secondAssetId)
    assertEquals(firstAssetId.hashCode, secondAssetId.hashCode)
    assertEquals(firstAssetId.toString, "AssetId(USD)")

    val version = validGridVersion(1)
    val gridId  = validGridId("cent")
    val _       = intercept[NullPointerException](GridKey(null, version))
    val _       = intercept[NullPointerException](GridIdentity(null, GridKey(gridId, version)))

    val local = GridKey(gridId, version)
    assertNotEquals(GridIdentity(DimKey.one, local), GridIdentity(DimKey.atom(AtomId("USD")), local))

  test("the synchronized bridge preserves idempotent registration, lookup, and conflicts"):
    val registry   = new QuantityRegistry
    val definition = assetDefinition("USD")
    val first      = registry.registerAsset(definition).toOption.get
    val second     = registry.registerAsset(definition).toOption.get

    assert(first.eq(second))
    assert(registry.resolveAsset(definition.id).contains(first))
    assertEquals(registry.registeredAssetCount, 1)
    assertEquals(registry.registeredDimensionCount, 1)
    assertEquals(
      registry.registerAsset(AssetDefinition(definition.id, AtomId("asset:other"))),
      Left(ConflictingAssetDefinition(definition.id, definition.dimensionAtom, AtomId("asset:other")))
    )

    val gridDefinitionValue = gridDefinition(first.dimension.key, "usd-cent")
    val grid                = registry.registerGrid(first)(gridDefinitionValue).toOption.get
    val sameGrid            = registry.registerGrid(first)(gridDefinitionValue).toOption.get
    assert(grid.eq(sameGrid))
    assert(registry.resolveGrid(first.dimension)(grid.key).contains(grid))
    assertEquals(registry.registeredGridCount, 1)

    val conflicting = gridDefinition(first.dimension.key, "usd-cent", PositiveRational.exact(3, 100).toOption.get)
    assertEquals(
      registry.registerGrid(first)(conflicting),
      Left(
        ConflictingGridDefinition(
          gridDefinitionValue.identity,
          cent.unrefined,
          conflicting.quantum.unrefined
        )
      )
    )

  test("grid handles delegate exactly to one retained anonymous mathematical grid"):
    val registry = new QuantityRegistry
    val asset    = registry.registerAsset(assetDefinition("USD-delegation")).toOption.get
    val handle   = registry.registerGrid(asset)(gridDefinition(asset.dimension.key, "delegating-cent")).toOption.get
    val value    = handle.fromCoordinate(BigInt(123))

    assertEquals(handle.coordinate(value), BigInt(123))
    assertEquals(handle.asQuantity(value), handle.grid.asQuantity(value))
    assertEquals(handle.asQuantity(value).coefficient, Rational(123, 100))
    assertEquals(handle.dimension.key, handle.grid.dimension.key)
    assertEquals(handle.quantum, handle.grid.quantum)

  test("pure handle reconciliation distinguishes lineage, identity, dimension, and mathematical compatibility"):
    val local       = new QuantityRegistry
    val foreign     = new QuantityRegistry
    val usd         = local.registerAsset(assetDefinition("USD-reconcile")).toOption.get
    val btc         = local.registerAsset(assetDefinition("BTC-reconcile")).toOption.get
    val foreignUsd  = foreign.registerAsset(assetDefinition("USD-reconcile")).toOption.get
    val cents       = local.registerGrid(usd)(gridDefinition(usd.dimension.key, "usd-cent-a")).toOption.get
    val otherCents  = local.registerGrid(usd)(gridDefinition(usd.dimension.key, "usd-cent-b")).toOption.get
    val foreignCent =
      foreign.registerGrid(foreignUsd)(gridDefinition(foreignUsd.dimension.key, "usd-cent-a")).toOption.get

    assert(DimensionHandle.reconcile(usd.dimension, usd.dimension).isRight)
    assertEquals(
      DimensionHandle.reconcile(usd.dimension, btc.dimension),
      Left(HandleDimensionMismatch(usd.dimension.key, btc.dimension.key))
    )
    assertEquals(
      DimensionHandle.sameLineage(usd.dimension, foreignUsd.dimension),
      Left(ForeignLineage(usd.dimension.key, foreignUsd.dimension.key))
    )
    assert(Asset.reconcile(usd, usd).isRight)
    assertEquals(
      Asset.reconcile(usd, foreignUsd),
      Left(ForeignLineage(usd.dimension.key, foreignUsd.dimension.key))
    )
    assert(GridHandle.reconcile(cents, cents).isRight)
    assertEquals(
      GridHandle.reconcile(cents, otherCents),
      Left(StableGridIdentityMismatch(cents.identity, otherCents.identity))
    )
    assertEquals(
      GridHandle.reconcile(cents, foreignCent),
      Left(ForeignLineage(cents.dimension.key, foreignCent.dimension.key))
    )
    assertEquals(SameGrid.between(cents.grid, otherCents.grid), Left(AnonymousGridMismatch))
    assert(SameQuantum.between(cents.grid, otherCents.grid).isRight)

  test("authority-bearing handles fail Java serialization and expose no construction mechanism"):
    val registry             = new QuantityRegistry
    val assetDefinitionValue = assetDefinition("USD-serialization")
    val asset                = registry.registerAsset(assetDefinitionValue).toOption.get
    val gridDefinitionValue  = gridDefinition(asset.dimension.key, "usd-serialization-cent")
    val grid                 = registry.registerGrid(asset)(gridDefinitionValue).toOption.get

    rejectSerialization(assetDefinitionValue.id)
    rejectSerialization(gridDefinitionValue.id)
    rejectSerialization(gridDefinitionValue.version)
    rejectSerialization(gridDefinitionValue.key)
    rejectSerialization(gridDefinitionValue.identity)
    rejectSerialization(assetDefinitionValue)
    rejectSerialization(gridDefinitionValue)
    rejectSerialization(asset)
    rejectSerialization(asset.dimension)
    rejectSerialization(grid)

    val forbiddenNames = Set("registerAsset", "registerDimension", "registerGrid", "resolveAsset", "resolveGrid")
    assertEquals(asset.getClass.getMethods.map(_.getName).toSet.intersect(forbiddenNames), Set.empty)
    assertEquals(grid.getClass.getMethods.map(_.getName).toSet.intersect(forbiddenNames), Set.empty)

  test("public reference-data roots reject null expected inputs before returning authority"):
    val registry                                = new QuantityRegistry
    val asset                                   = registry.registerAsset(assetDefinition("USD-null")).toOption.get
    val nullDimension: DimensionHandle[asset.D] = null
    val nullAsset: Asset                        = null
    val nullGrid: GridHandle[asset.D]           = null

    val _ = intercept[NullPointerException](registry.registerAsset(null))
    val _ = intercept[NullPointerException](registry.resolveAsset(null))
    val _ = intercept[NullPointerException](registry.registerDimension(null))
    val _ = intercept[NullPointerException](registry.resolveDimension(null))
    val _ = intercept[NullPointerException](
      registry.registerGrid(nullDimension)(gridDefinition(asset.dimension.key, "null-grid"))
    )
    val _ = intercept[NullPointerException](registry.registerGrid(asset)(null))
    val _ = intercept[NullPointerException](registry.resolveGrid(asset.dimension)(null))
    val _ = intercept[NullPointerException](DimensionHandle.sameLineage(nullDimension, asset.dimension))
    val _ = intercept[NullPointerException](DimensionHandle.reconcile(asset.dimension, nullDimension))
    val _ = intercept[NullPointerException](Asset.reconcile(nullAsset, asset))
    val _ = intercept[NullPointerException](GridHandle.reconcile(nullGrid, nullGrid))

end ReferenceDataSuite
