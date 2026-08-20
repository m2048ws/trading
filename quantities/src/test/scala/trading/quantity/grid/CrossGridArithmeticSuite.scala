package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*

class CrossGridArithmeticSuite extends ScalaCheckSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-cross-grid-suite"
  private val cents =
    UniformGrid.create[usd.D](
      GridId:
        "usd-cent-cross-grid-suite"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )
  private val threeCents =
    UniformGrid.create[usd.D](
      GridId:
        "usd-three-cent-cross-grid-suite"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(3, 100).toOption.get
    )

  property("cross-grid addition and subtraction return exact quantities"):
    forAll(coordinate, coordinate): (centCoordinate, threeCentCoordinate) =>
      val centValue =
        cents.fromCoordinate:
          centCoordinate
      val threeCentValue =
        threeCents.fromCoordinate:
          threeCentCoordinate
      val left  = Rational(centCoordinate, 100)
      val right = Rational(threeCentCoordinate * 3, 100)

      assertEquals(centValue.addExact(threeCentValue, cents, threeCents).coefficient, left + right)
      assertEquals(centValue.subtractExact(threeCentValue, cents, threeCents).coefficient, left - right)

  test("cross-grid equality compares mathematical values, not coordinates or identities"):
    val threeCentValue = threeCents.fromCoordinate(1)
    assert:
      cents.fromCoordinate(3).exactlyEquals(threeCentValue, cents, threeCents)
    assert:
      !cents.fromCoordinate(1).exactlyEquals(threeCentValue, cents, threeCents)

  property("cross-grid comparison uses exact mathematical ordering"):
    forAll(coordinate, coordinate): (centCoordinate, threeCentCoordinate) =>
      val centValue =
        cents.fromCoordinate:
          centCoordinate
      val threeCentValue =
        threeCents.fromCoordinate:
          threeCentCoordinate
      val expected =
        Rational(centCoordinate, 100).compare:
          Rational(threeCentCoordinate * 3, 100)

      assertEquals(centValue.compareExact(threeCentValue, cents, threeCents), expected)

  test("equivalence-aware comparison retains distinct static dimension spellings"):
    val a = DimRef.atomic(AtomId("cross-grid-comparison:a"))
    val b = DimRef.atomic(AtomId("cross-grid-comparison:b"))
    type AB = Dim[Power[a.type, 1] *: Power[b.type, 1] *: EmptyTuple]
    type BA = Dim[Power[b.type, 1] *: Power[a.type, 1] *: EmptyTuple]
    val ab: DimRef[AB] = DimRef.times(a.dimension, b.dimension)
    val ba: DimRef[BA] = DimRef.times(b.dimension, a.dimension)
    val abGrid         = UniformGrid.create(
      GridId("cross-grid-comparison:ab"),
      GridVersion(1),
      ab,
      PositiveRational.exact(1, 10).toOption.get
    )
    val baGrid = UniformGrid.create(
      GridId("cross-grid-comparison:ba"),
      GridVersion(1),
      ba,
      PositiveRational.exact(1, 5).toOption.get
    )
    val left         = abGrid.fromCoordinate(2)
    val equalRight   = baGrid.fromCoordinate(1)
    val greaterRight = baGrid.fromCoordinate(2)

    assert(left.exactlyEquals(equalRight, abGrid, baGrid))
    assertEquals(left.compareExact(equalRight, abGrid, baGrid), 0)
    assertEquals(left.compareExact(greaterRight, abGrid, baGrid), -1)

end CrossGridArithmeticSuite
