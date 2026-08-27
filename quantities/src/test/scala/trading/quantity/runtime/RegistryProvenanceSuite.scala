package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.CompileAssertions.*

class RegistryProvenanceSuite extends FunSuite:
  test("runtime asset and dimension witnesses cannot be user-implemented"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.runtime.*
      val forged = new AssetRef:
        type D = One
        val id = AssetId("forged")
        val dimension: RegisteredDimensionRef[D] = ???
    """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.runtime.*
      val forged = new RegisteredDimensionRef[One]:
        val key = DimKey.one
        val asDimensionRef = DimRef.one
        def sharesRegistryWith(r: RegisteredDimensionRef[? <: Dim]): Boolean = true
    """

  test("a registry rejects dimension witnesses owned by another registry"):
    val first   = new QuantityRegistry
    val foreign =
      first.registerAsset(AssetDefinition(AssetId("foreign-owner"), AtomId("asset:foreign-owner"))).toOption.get
    val second = new QuantityRegistry
    val local  =
      second.registerAsset(AssetDefinition(AssetId("foreign-owner"), AtomId("asset:foreign-owner"))).toOption.get
    val definition =
      GridDefinition(
        local.dimension.key,
        GridId("foreign-grid"),
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )

    assertEquals(second.registerGrid(foreign)(definition), Left(ForeignDimensionWitness(foreign.dimension.key)))
    assertEquals(second.resolveGrid(foreign)(definition.key), Left(ForeignDimensionWitness(foreign.dimension.key)))

end RegistryProvenanceSuite
