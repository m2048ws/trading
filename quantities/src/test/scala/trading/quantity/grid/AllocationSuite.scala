package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*
import trading.quantity.testkit.TestAsset

class AllocationSuite extends ScalaCheckSuite:
  private val asset = TestAsset.runtime(AtomId("allocation-laws"))
  private val grid  =
    UniformGrid.create(asset.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )

  private def quantity(v: BigInt): GridQuantity[asset.D, grid.G] =
    grid.fromCoordinate(v)

  property("even allocation conserves coordinates with at-most-one-quantum spread"):
    val countGenerator =
      Gen
        .chooseNum(1, 100)
        .map(value => PositiveInt(value).toOption.get)

    forAll(
      coordinate,
      countGenerator,
      Gen.oneOf:
        RemainderOrder.values.toIndexedSeq
    ): (sourceCoordinate, count, order) =>
      val allocation  = quantity(sourceCoordinate).allocateEvenly(count, order, grid)
      val coordinates =
        allocation.parts
          .map:
            grid.coordinate

      assertEquals(allocation.size, count.unrefined)
      assertEquals(coordinates.sum, sourceCoordinate)
      assert:
        coordinates.max - coordinates.min <= 1

  test("remainder order explicitly and stably selects extra-quantum recipients"):
    val count  = PositiveInt(3).toOption.get
    val source =
      quantity:
        1000

    assertEquals(
      source
        .allocateEvenly(count, RemainderOrder.FirstToLast, grid)
        .parts
        .map:
          grid.coordinate
      ,
      Vector(
        BigInt:
          334
        ,
        BigInt:
          333
        ,
        BigInt:
          333
      )
    )
    assertEquals(
      source
        .allocateEvenly(count, RemainderOrder.LastToFirst, grid)
        .parts
        .map:
          grid.coordinate
      ,
      Vector(
        BigInt:
          333
        ,
        BigInt:
          333
        ,
        BigInt:
          334
      )
    )

  test("negative totals retain Euclidean allocation semantics"):
    val count      = PositiveInt(3).toOption.get
    val allocation = quantity(-1000).allocateEvenly(count, RemainderOrder.FirstToLast, grid)

    assertEquals(
      allocation.parts
        .map:
          grid.coordinate
      ,
      Vector(
        BigInt:
          -333
        ,
        BigInt:
          -333
        ,
        BigInt:
          -334
      )
    )

end AllocationSuite
