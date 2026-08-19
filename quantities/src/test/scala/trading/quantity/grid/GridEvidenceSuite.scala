package trading.quantity.grid

import munit.ScalaCheckSuite

import trading.quantity.*
import trading.quantity.refinement.*

class GridEvidenceSuite extends ScalaCheckSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-grid-relationships-suite"
  private val btc =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "BTC-grid-relationships-suite"
  private val quantum = PositiveRational.exact(1, 100).toOption.get

  private val venueA =
    UniformGrid.create[usd.D](
      GridId:
        "venue-a-cent"
      ,
      GridVersion(1),
      usd.dimension,
      quantum
    )
  private val venueASecondPath =
    UniformGrid.create[usd.D](
      GridId:
        "venue-a-cent"
      ,
      GridVersion(1),
      usd.dimension,
      quantum
    )
  private val venueB =
    UniformGrid.create[usd.D](
      GridId:
        "venue-b-cent"
      ,
      GridVersion(1),
      usd.dimension,
      quantum
    )
  private val btcCents =
    UniformGrid.create[btc.D](
      GridId:
        "btc-cent"
      ,
      GridVersion(1),
      btc.dimension,
      quantum
    )

  test("same-grid evidence is recovered across separate stable witness paths"):
    val evidence = SameGrid.between(venueA, venueASecondPath).toOption.get
    val source   =
      venueA.fromCoordinate:
        123
    val retyped: GridQuantity[usd.D, venueASecondPath.G] =
      evidence.retype:
        source

    assertEquals(
      venueASecondPath.coordinate:
        retyped
      ,
      BigInt:
        123
    )

  test("dimension evidence preserves grid identity and does not replace same-grid evidence"):
    val leftDimension  = DimRef.atomic(AtomId("grid-dimension-evidence"))
    val rightDimension = DimRef.atomic(AtomId("grid-dimension-evidence"))
    val left    = UniformGrid.create(GridId("grid-dimension-left"), GridVersion(1), leftDimension.dimension, quantum)
    val right   = UniformGrid.create(GridId("grid-dimension-right"), GridVersion(1), rightDimension.dimension, quantum)
    val checked = SameDimension.between(leftDimension.dimension, rightDimension.dimension).get
    val source  = left.fromCoordinate(9)
    val aligned: GridQuantity[rightDimension.D, left.G] =
      source.asDimension[rightDimension.D](using checked)
    val reverse  = SameDimension.between(rightDimension.dimension, leftDimension.dimension).get
    val restored = aligned.asDimension[leftDimension.D](using reverse)

    assertEquals(left.coordinate(restored), BigInt(9))
    assertEquals(SameGrid.between(left, right), Left(GridIdentityMismatch))

  test("equal quantum on different grids is numerical compatibility, not identity"):
    assertEquals(
      SameGrid.between(venueA, venueB),
      Left:
        GridIdentityMismatch
    )

    val compatibility = SameQuantum.between(venueA, venueB).toOption.get
    val converted     =
      compatibility.convert:
        venueA.fromCoordinate:
          42
    assertEquals(
      venueB.coordinate:
        converted
      ,
      BigInt:
        42
    )

  test("equal quanta do not bypass dimension identity"):
    assertEquals(
      SameGrid.between(venueA, btcCents),
      Left:
        GridDimensionMismatch
    )
    assertEquals(
      SameQuantum.between(venueA, btcCents),
      Left:
        GridDimensionMismatch
    )

  test("conflicting immutable definitions cannot yield same-grid evidence"):
    val conflicting =
      UniformGrid.create[usd.D](venueA.id, venueA.version, usd.dimension, PositiveRational.exact(3, 100).toOption.get)

    assertEquals(
      SameGrid.between(venueA, conflicting),
      Left:
        GridDefinitionConflict
    )

  test("three-cent values embed globally and exactly into cents"):
    val threeCents =
      UniformGrid.create[usd.D](
        GridId:
          "three-cent-embedding-suite"
        ,
        GridVersion(1),
        usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )
    val embedding = Embedding.between(threeCents, venueA).toOption.get
    val widened   =
      embedding.widenTo:
        threeCents.fromCoordinate(2)

    assertEquals(embedding.coordinateFactor, BigInt(3))
    assertEquals(
      venueA.coordinate:
        widened
      ,
      BigInt(6)
    )

  test("cent values do not globally embed into the three-cent grid"):
    val threeCents =
      UniformGrid.create[usd.D](
        GridId:
          "three-cent-rejected-embedding-suite"
        ,
        GridVersion(1),
        usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )

    assertEquals(
      Embedding.between(venueA, threeCents),
      Left:
        NoGridEmbedding
    )

end GridEvidenceSuite
