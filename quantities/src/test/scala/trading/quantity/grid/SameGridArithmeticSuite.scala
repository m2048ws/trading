package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*
import trading.quantity.testkit.TestAsset

class SameGridArithmeticSuite extends ScalaCheckSuite:
  private val asset = TestAsset.runtime(AssetId("same-grid-laws"))
  private val grid  =
    UniformGrid.create(
      GridId("same-grid-laws"),
      GridVersion(1),
      asset.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  private def quantity(c: BigInt): GridQuantity[asset.D, grid.G] =
    grid.fromCoordinate(c)

  property("same-grid addition, subtraction, negation, and scaling use exact coordinates"):
    forAll(
      coordinate,
      coordinate,
      Gen
        .chooseNum(-1000L, 1000L)
        .map:
          BigInt(_)
    ): (leftCoordinate, rightCoordinate, scalar) =>
      val left =
        quantity:
          leftCoordinate
      val right =
        quantity:
          rightCoordinate

      assertEquals(grid.coordinate(left + right), leftCoordinate + rightCoordinate)
      assertEquals(grid.coordinate(left - right), leftCoordinate - rightCoordinate)
      assertEquals(grid.coordinate(-left), -leftCoordinate)
      assertEquals(grid.coordinate(left * scalar), leftCoordinate * scalar)

  property("zero is the additive identity without grid metadata"):
    forAll(coordinate): value =>
      val quantityValue =
        quantity:
          value
      val zero = GridQuantity.zero[asset.D, grid.G](using asset.dimension)

      assertEquals(grid.coordinate(zero), BigInt(0))
      assert:
        (quantityValue + zero).sameGridEquals:
          quantityValue
      assert:
        (zero + quantityValue).sameGridEquals:
          quantityValue

  property("same-grid equality, hashing, comparison, and ordering are coordinate based"):
    forAll(coordinate, coordinate): (leftCoordinate, rightCoordinate) =>
      val left =
        quantity:
          leftCoordinate
      val sameLeft =
        quantity:
          leftCoordinate
      val right =
        quantity:
          rightCoordinate
      val expectedComparison =
        leftCoordinate.compare:
          rightCoordinate

      assert:
        left.sameGridEquals:
          sameLeft
      assertEquals(
        left.sameGridEquals:
          right
        ,
        leftCoordinate == rightCoordinate
      )
      assertEquals(left.sameGridHash, sameLeft.sameGridHash)
      assertEquals(
        left.compareSameGrid:
          right
        ,
        expectedComparison
      )
      assertEquals(summon[Ordering[GridQuantity[asset.D, grid.G]]].compare(left, right), expectedComparison)

end SameGridArithmeticSuite
