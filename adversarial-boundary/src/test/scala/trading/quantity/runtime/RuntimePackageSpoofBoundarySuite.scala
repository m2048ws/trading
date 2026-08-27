package trading.quantity.runtime

import scala.compiletime.testing.typeCheckErrors

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class RuntimePackageSpoofBoundarySuite extends FunSuite:

  inline def assertRejected(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.isEmpty then
      fail("expected downstream runtime-package source to be rejected")

  inline def assertAccepted(inline code: String): Unit =
    val errors = typeCheckErrors(code)
    if errors.nonEmpty then
      fail(s"expected downstream prelude to compile: ${errors.map(_.message).mkString("; ")}")

  test("supported runtime-package registry prelude compiles"):
    assertAccepted:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.runtime.*
      val registry = new QuantityRegistry
      val asset = registry.registerAsset(
        AssetDefinition(AssetId("runtime-package-prelude"), AtomId("runtime-package-prelude"))
      ).toOption.get
      val definition = GridDefinition(asset.dimension.key, GridId("runtime-package-prelude"), GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get)
      val grid = registry.registerGrid(asset)(definition).toOption.get
      val value = grid.fromCoordinate(42)
      val packed = PackedAssetGridQuantity.pack(asset)(grid)(value)
      """

  test("same-package source cannot name or instantiate registry-owned implementations"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.runtime.*
      val registry = new QuantityRegistry
      val forged = new registry.InternedRegisteredDimensionRef(DimRef.one)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.runtime.*
      val registry = new QuantityRegistry
      val dimension = registry.registerDimension(DimKey.one).toOption.get
      val plain = UniformGrid.create(GridId("private-grid"), GridVersion(1), dimension.dimension.asDimensionRef,
        PositiveRational.exact(1, 100).toOption.get)
      val forged = new registry.InternedRegisteredGridRef(dimension.dimension, plain)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.runtime.*
      val registry = new QuantityRegistry
      val forged = new registry.InternedAssetRef(AssetId("private-asset"), AtomId("private-asset"))
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.runtime.*
      val registry = new QuantityRegistry
      val forged = new registry.InternedDimensionWitness(DimKey.one)
      """

  test("same-package source cannot claim registry ownership or promote plain witnesses"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.runtime.*
      val forged = new RegisteredDimensionRef[One]:
        val key = DimKey.one
        val asDimensionRef = DimRef.one
        def sharesRegistryWith(r: RegisteredDimensionRef[? <: Dim]): Boolean = true
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.runtime.*
      val plain = UniformGrid.create(GridId("plain-grid"), GridVersion(1), DimRef.one,
        PositiveRational.exact(1, 100).toOption.get)
      val forged: RegisteredGridRef[One] = plain
      """

  test("counterfeit quantum cannot acquire registered provenance or pack"):
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.runtime.*
      val registry = new QuantityRegistry
      val asset = registry.registerAsset(
        AssetDefinition(AssetId("counterfeit-asset"), AtomId("counterfeit-dimension"))
      ).toOption.get
      val id = GridId("counterfeit-grid")
      val version = GridVersion(1)
      val canonical = registry.registerGrid(asset)(
        GridDefinition(asset.dimension.key, id, version, PositiveRational.exact(1, 100).toOption.get)
      ).toOption.get
      val counterfeit = UniformGrid.create(id, version, asset.dimension.asDimensionRef,
        PositiveRational.exact(7, 13).toOption.get)
      val coordinate = counterfeit.fromCoordinate(42)
      val forged = new registry.InternedRegisteredGridRef(asset.dimension, counterfeit)
      """
    assertRejected:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.runtime.*
      val registry = new QuantityRegistry
      val asset = registry.registerAsset(
        AssetDefinition(AssetId("counterfeit-pack-asset"), AtomId("counterfeit-pack-dimension"))
      ).toOption.get
      val id = GridId("counterfeit-pack-grid")
      val version = GridVersion(1)
      val canonical = registry.registerGrid(asset)(
        GridDefinition(asset.dimension.key, id, version, PositiveRational.exact(1, 100).toOption.get)
      ).toOption.get
      val counterfeit = UniformGrid.create(id, version, asset.dimension.asDimensionRef,
        PositiveRational.exact(7, 13).toOption.get)
      val coordinate = counterfeit.fromCoordinate(42)
      val packed = PackedAssetGridQuantity.pack(asset)(counterfeit)(coordinate)
      """

  test("legitimate registry provenance preserves coordinate 42 and canonical value"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset(AssetDefinition(AssetId("canonical-asset"), AtomId("canonical-dimension")))
        .toOption
        .get
    val definition =
      GridDefinition(
        asset.dimension.key,
        GridId("canonical-grid"),
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val grid     = registry.registerGrid(asset)(definition).toOption.get
    val packed   = PackedAssetGridQuantity.pack(asset)(grid)(grid.fromCoordinate(42))
    val resolved = PackedAssetGridQuantity.decode(packed, registry).toOption.get

    assertEquals(resolved.grid.coordinate(resolved.value), BigInt(42))
    assertEquals(resolved.grid.asQuantity(resolved.value).coefficient, Rational(21, 50))

end RuntimePackageSpoofBoundarySuite
