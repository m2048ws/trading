package trading.quantity.grid

import munit.ScalaCheckSuite

import trading.quantity.*
import trading.quantity.refinement.*

class GridEvidenceSuite extends ScalaCheckSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "USD-grid-relationships-suite"
  private val btc =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "BTC-grid-relationships-suite"
  private val quantum = PositiveRational.exact(1, 100).toOption.get

  private val venueA =
    UniformGrid.create[usd.D](usd.dimension,
      quantum
    )
  private val venueASecondPath: GridRef[usd.D] = venueA
  private val venueB                           =
    UniformGrid.create[usd.D](usd.dimension,
      quantum
    )
  private val btcCents =
    UniformGrid.create[btc.D](btc.dimension,
      quantum
    )

  test("same-grid evidence is recovered across retained paths to one anonymous grid"):
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
    val left           = UniformGrid.create(leftDimension.dimension, quantum)
    val right          = UniformGrid.create(rightDimension.dimension, quantum)
    val checked        = SameDimension.between(leftDimension.dimension, rightDimension.dimension).get
    val source         = left.fromCoordinate(9)
    val aligned: GridQuantity[rightDimension.D, left.G] =
      source.alignTo[rightDimension.D](using checked)
    val reverse  = SameDimension.between(rightDimension.dimension, leftDimension.dimension).get
    val restored = aligned.alignTo[leftDimension.D](using reverse)

    assertEquals(left.coordinate(restored), BigInt(9))
    assertEquals(SameGrid.between(left, right), Left(AnonymousGridMismatch))

  test("equal quantum on different grids is numerical compatibility, not identity"):
    assertEquals(
      SameGrid.between(venueA, venueB),
      Left:
        AnonymousGridMismatch
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

  test("separate different-quantum grids yield neither same-grid nor same-quantum evidence"):
    val differentQuantum =
      UniformGrid.create[usd.D](usd.dimension, PositiveRational.exact(3, 100).toOption.get)

    assertEquals(
      SameGrid.between(venueA, differentQuantum),
      Left:
        AnonymousGridMismatch
    )
    assertEquals(
      SameQuantum.between(venueA, differentQuantum),
      Left:
        GridQuantumMismatch
    )

  test("three-cent values embed globally and exactly into cents"):
    val threeCents =
      UniformGrid.create[usd.D](usd.dimension,
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
      UniformGrid.create[usd.D](usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )

    assertEquals(
      Embedding.between(venueA, threeCents),
      Left:
        NoGridEmbedding
    )

end GridEvidenceSuite
