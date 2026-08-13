package external

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class PublicApiBoundarySuite extends FunSuite:
  test("client code cannot implement dimension or grid witnesses"):
    assertDoesNotCompile:
      """
      import trading.quantity.*

      val forged = new DimRef[One]:
        val key = DimensionKey.one
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*

      val asset = trading.quantity.testkit.TestAsset.runtime(AssetId("forged-grid-asset"))
      val forged = new GridRef[asset.D]:
        type G = this.type
        val id = GridId("forged-grid")
        val version = GridVersion(1)
        val dimension = asset.dimension
        val quantum = PositiveRational.exact(1, 100).toOption.get
    """

  test("runtime asset, dimension, and grid witness roles cannot be interchanged"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.runtime.*

      def requireGrid[D <: Dimension](v: RegisteredGridRef[D]): Unit = ()
      val registry = new QuantityRegistry
      val asset = registry
        .registerAsset(AssetDefinition(AssetId("witness-role-asset"), AtomId("witness-role-atom")))
        .toOption.get
      requireGrid(asset)
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.runtime.*

      def requireAsset(v: trading.quantity.runtime.AssetRef): Unit = ()
      val registry = new QuantityRegistry
      val asset = registry
        .registerAsset(AssetDefinition(AssetId("witness-role-grid"), AtomId("witness-role-grid-atom")))
        .toOption.get
      val grid = registry.registerGrid(asset)(GridDefinition(
        asset.dimension.key, GridId("witness-role-grid-id"), GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )).toOption.get
      requireAsset(grid)
    """

  test("packed coordinates cannot be treated as typed quantities before decoding"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.runtime.*

      val registry = new QuantityRegistry
      val asset = registry
        .registerAsset(AssetDefinition(AssetId("raw-coordinate-asset"), AtomId("raw-coordinate-atom")))
        .toOption.get
      val grid = registry.registerGrid(asset)(GridDefinition(
        asset.dimension.key, GridId("raw-coordinate-grid"), GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )).toOption.get
      val packed = PackedAssetGridQuantity(asset.id, asset.dimension.key, grid.id, grid.version, BigInt(1000))
      val invalid: GridQuantity[asset.D, grid.G] = packed
    """

  test("client code cannot supply an unchecked quantization policy"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.grid.*

      val unchecked = new QuantizationPolicy:
        def roundCoordinate(v: Rational): BigInt = 0
        def acceptsResidual(v: Rational, c: BigInt): Boolean = true
    """

  test("canonical value constructors cannot be bypassed through case-class helpers"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val nonCanonical = Rational.fromProduct((BigInt(2), BigInt(2)))
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*
      val nonCanonical = DimensionKey.fromProduct(
        Tuple1(Vector(AtomId("duplicate") -> 1, AtomId("duplicate") -> 1))
      )
    """

end PublicApiBoundarySuite
