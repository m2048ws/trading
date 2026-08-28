package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*
import trading.quantity.testkit.TestAsset

class QuotRemSuite extends ScalaCheckSuite:
  private val asset = TestAsset.runtime(AtomId("quot-rem-laws"))
  private val grid  =
    UniformGrid.create(asset.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  private def quantity(v: BigInt): GridQuantity[asset.D, grid.G] =
    grid.fromCoordinate(v)

  property("quotRemBy satisfies Euclidean conservation and remainder bounds"):
    val positiveDivisor =
      Gen
        .chooseNum(1L, 1_000_000L)
        .map(value => PositiveWhole(value).toOption.get)

    forAll(coordinate, positiveDivisor): (sourceCoordinate, divisor) =>
      val result    = quantity(sourceCoordinate).quotRemBy(divisor, grid)
      val quotient  = grid.coordinate(result.quotient)
      val remainder = grid.coordinate(result.remainder)

      assertEquals(sourceCoordinate, divisor.unrefined * quotient + remainder)
      assert:
        remainder >= 0
      assert:
        remainder < divisor.unrefined

  test("negative coordinates use Euclidean rather than truncating division"):
    val divisor = PositiveWhole(3).toOption.get
    val result  = quantity(-1000).quotRemBy(divisor, grid)

    assertEquals(
      grid.coordinate(result.quotient),
      BigInt:
        -334
    )
    assertEquals(grid.coordinate(result.remainder), BigInt(2))

end QuotRemSuite
