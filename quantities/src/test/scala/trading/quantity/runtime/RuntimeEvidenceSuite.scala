package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.GridIdentityMismatch
import trading.quantity.refinement.*

class RuntimeEvidenceSuite extends FunSuite:

  private def assetDefinition(id: String): AssetDefinition =
    AssetDefinition(
      AssetId:
        id
      ,
      AtomId:
        s"asset:$id"
    )

  test("same-dimension evidence recovers across separately typed stable paths"):
    val registry = new QuantityRegistry
    val left     =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-usd"
        .toOption
        .get
    val right =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-usd"
        .toOption
        .get
    val evidence                      = RuntimeEvidence.sameDimension(left, right).toOption.get
    val leftValue                     = Quantity(left.dimension.asDimensionRef, 10)
    val rightValue: Quantity[right.D] =
      evidence.coerceQuantity:
        leftValue

    assertEquals(
      rightValue.coefficient,
      Rational:
        10
    )

  test("same-dimension evidence rejects distinct canonical assets"):
    val registry = new QuantityRegistry
    val left     =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-usd-left"
        .toOption
        .get
    val right =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-usd-right"
        .toOption
        .get

    assertEquals(
      RuntimeEvidence.sameDimension(left, right),
      Left:
        DimensionEvidenceMismatch(left.dimension.key, right.dimension.key)
    )

  test("same-grid evidence recovers across separately typed stable grid paths"):
    val registry  = new QuantityRegistry
    val leftAsset =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-grid-usd"
        .toOption
        .get
    val rightAsset =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-grid-usd"
        .toOption
        .get
    val definition =
      GridDefinition(
        leftAsset.dimension.key,
        GridId:
          "evidence-grid-cent"
        ,
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val leftGrid =
      registry
        .registerGrid(leftAsset):
          definition
        .toOption
        .get
    val rightGrid =
      registry
        .registerGrid(rightAsset):
          definition
        .toOption
        .get
    val evidence  = RuntimeEvidence.sameGrid(leftGrid, rightGrid).toOption.get
    val leftValue =
      leftGrid.fromCoordinate:
        123
    val rightValue: GridQuantity[rightAsset.D, rightGrid.G] =
      evidence.retype:
        leftValue

    assertEquals(
      rightGrid.coordinate:
        rightValue
      ,
      BigInt:
        123
    )

  test("equal-quantum grids with different identities do not yield same-grid evidence"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-venues-usd"
        .toOption
        .get
    val quantum = PositiveRational.exact(1, 100).toOption.get
    val left    =
      registry
        .registerGrid(asset):
          GridDefinition(
            asset.dimension.key,
            GridId:
              "evidence-venue-a"
            ,
            GridVersion(1),
            quantum
          )
        .toOption
        .get
    val right =
      registry
        .registerGrid(asset):
          GridDefinition(
            asset.dimension.key,
            GridId:
              "evidence-venue-b"
            ,
            GridVersion(1),
            quantum
          )
        .toOption
        .get

    assertEquals(
      RuntimeEvidence.sameGrid(left, right),
      Left:
        GridEvidenceMismatch:
          GridIdentityMismatch
    )

  test("equal-quantum versions remain distinct grid identities"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          assetDefinition:
            "evidence-versioned-usd"
        .toOption
        .get
    val id =
      GridId:
        "evidence-versioned-cent"
    val quantum    = PositiveRational.exact(1, 100).toOption.get
    val versionOne =
      registry
        .registerGrid(asset):
          GridDefinition(asset.dimension.key, id, GridVersion(1), quantum)
        .toOption
        .get
    val versionTwo =
      registry
        .registerGrid(asset):
          GridDefinition(asset.dimension.key, id, GridVersion(2), quantum)
        .toOption
        .get

    assertEquals(
      RuntimeEvidence.sameGrid(versionOne, versionTwo),
      Left:
        GridEvidenceMismatch:
          GridIdentityMismatch
    )

  test("normalized compound dimensions recover checked evidence"):
    val registry = new QuantityRegistry
    val expanded =
      DimensionKey:
        List(
          AtomId:
            "compound-usd"
          -> BigInt(2),
          AtomId:
            "compound-btc"
          -> BigInt(-1),
          AtomId:
            "compound-usd"
          -> BigInt(-1)
        )
    val reduced =
      DimensionKey:
        List(
          AtomId:
            "compound-usd"
          -> BigInt(1),
          AtomId:
            "compound-btc"
          -> BigInt(-1)
        )
    val left =
      registry
        .registerDimension:
          expanded
        .toOption
        .get
    val right =
      registry
        .registerDimension:
          reduced
        .toOption
        .get
    val evidence                    = RuntimeEvidence.sameDimension(left, right).toOption.get
    val source                      = Quantity(left.dimension.asDimensionRef, Rational(3, 2))
    val restored: Quantity[right.D] =
      evidence.coerceQuantity:
        source

    assertEquals(restored.coefficient, Rational(3, 2))

  test("runtime evidence rejects witnesses owned by different registries"):
    val firstRegistry  = new QuantityRegistry
    val secondRegistry = new QuantityRegistry
    val definition     = assetDefinition("foreign-evidence-asset")
    val firstAsset     = firstRegistry.registerAsset(definition).toOption.get
    val secondAsset    = secondRegistry.registerAsset(definition).toOption.get
    val gridDefinition =
      GridDefinition(
        firstAsset.dimension.key,
        GridId("foreign-evidence-grid"),
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val firstGrid  = firstRegistry.registerGrid(firstAsset)(gridDefinition).toOption.get
    val secondGrid = secondRegistry.registerGrid(secondAsset)(gridDefinition).toOption.get
    val expected   =
      Left:
        ForeignRegistryEvidence(firstAsset.dimension.key, secondAsset.dimension.key)

    assertEquals(RuntimeEvidence.sameDimension(firstAsset, secondAsset), expected)
    assertEquals(RuntimeEvidence.sameGrid(firstGrid, secondGrid), expected)

end RuntimeEvidenceSuite
