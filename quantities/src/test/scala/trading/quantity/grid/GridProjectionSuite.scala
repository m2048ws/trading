package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.coordinate

class GridProjectionSuite extends ScalaCheckSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-grid-projection-suite"
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

  test("six cents narrows exactly to coordinate two on a three-cent grid"):
    val threeCents =
      UniformGrid.create[usd.D](
        GridId:
          "three-cent-narrowing-suite"
        ,
        GridVersion(1),
        usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )
    val exactSixCents = Quantity(usd.dimension, Rational(6, 100))
    val narrowed      =
      exactSixCents
        .narrowExactlyTo:
          threeCents
        .toOption
        .get

    assertEquals(
      threeCents.coordinate:
        narrowed
      ,
      BigInt(2)
    )

  test("one cent returns a structured not-on-three-cent-grid error"):
    val threeCents =
      UniformGrid.create[usd.D](
        GridId:
          "three-cent-rejected-narrowing-suite"
        ,
        GridVersion(1),
        usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )
    val exactOneCent = Quantity(usd.dimension, Rational(1, 100))
    val expected: Either[NotOnGrid[usd.D], GridQuantity[usd.D, threeCents.G]] =
      Left:
        NotOnGrid[usd.D](Rational(1, 100), threeCents.key, Rational(3, 100))

    assertEquals(
      exactOneCent.narrowExactlyTo:
        threeCents
      ,
      expected
    )

  test("grid quantities can narrow value-specifically through exact interpretation"):
    val threeCents =
      UniformGrid.create[usd.D](
        GridId:
          "three-cent-grid-narrowing-suite"
        ,
        GridVersion(1),
        usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )

    assertEquals(
      venueA
        .fromCoordinate(6)
        .narrowExactlyTo(venueA, threeCents)
        .map:
          threeCents.coordinate
      ,
      Right:
        BigInt(2)
    )
    assert:
      venueA.fromCoordinate(1).narrowExactlyTo(venueA, threeCents).isLeft

  property("exact narrowing rejects every nonrepresentable three-cent value"):
    val threeCents =
      UniformGrid.create[usd.D](
        GridId:
          "three-cent-property-narrowing-suite"
        ,
        GridVersion(1),
        usd.dimension,
        PositiveRational.exact(3, 100).toOption.get
      )

    forAll(coordinate): coordinate =>
      val offGrid = Quantity(usd.dimension, Rational(coordinate * 3 + 1, 100))

      assert:
        offGrid
          .narrowExactlyTo:
            threeCents
          .isLeft
      assertEquals(offGrid.coefficient, Rational(coordinate * 3 + 1, 100))

end GridProjectionSuite
