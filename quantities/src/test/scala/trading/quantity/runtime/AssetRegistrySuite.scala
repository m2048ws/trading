package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*

class AssetRegistrySuite extends FunSuite:
  test("runtime identifiers resolve to stable typed asset witnesses"):
    val registry                    = new QuantityRegistry
    val identifierFromConfiguration =
      AssetId:
        "runtime-usd"
    val registered =
      registry
        .registerAsset:
          AssetDefinition(
            identifierFromConfiguration,
            AtomId:
              "asset:runtime-usd"
          )
        .toOption
        .get
    val resolved =
      registry
        .resolveAsset:
          identifierFromConfiguration
        .toOption
        .get

    assert:
      registered
        .asInstanceOf[AnyRef]
        .eq:
          resolved.asInstanceOf[AnyRef]
    val value: Quantity[resolved.D] = Quantity(resolved.dimension.ref, 10)
    assertEquals(
      value.coefficient,
      Rational:
        10
    )

  test("conflicting immutable asset definitions are rejected"):
    val registry = new QuantityRegistry
    val id       =
      AssetId:
        "runtime-conflict"
    val first =
      AssetDefinition(
        id,
        AtomId:
          "asset:runtime-conflict:v1"
      )
    val conflict =
      AssetDefinition(
        id,
        AtomId:
          "asset:runtime-conflict:different"
      )

    assert:
      registry
        .registerAsset:
          first
        .isRight
    assertEquals(
      registry.registerAsset:
        conflict
      ,
      Left:
        ConflictingAssetDefinition(id, first.dimensionAtom, conflict.dimensionAtom)
    )

  test("many runtime assets require no source-defined phantom tags"):
    val registry = new QuantityRegistry
    1.to:
      1000
    .foreach: index =>
      val id =
        AssetId:
          s"runtime-asset-$index"
      assert:
        registry
          .registerAsset:
            AssetDefinition(
              id,
              AtomId:
                s"asset:$index"
            )
          .isRight
    assertEquals(registry.registeredAssetCount, 1000)

end AssetRegistrySuite
