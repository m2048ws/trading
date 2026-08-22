package external.fixtures.negative

import trading.quantity.*
import trading.quantity.refinement.*

object ImplicitEquivalentArithmetic:
  val a = DimRef.atomic(AtomId("implicit-equivalent:a"))
  val b = DimRef.atomic(AtomId("implicit-equivalent:b"))

  type AB = Times[a.D, b.D]
  type BA = Times[b.D, a.D]

  val ab: DimRef[AB] = DimRef.times(a.dimension, b.dimension)
  val ba: DimRef[BA] = DimRef.times(b.dimension, a.dimension)
  val left = Quantity(ab, 2)
  val right = Quantity(ba, 3)

  val leftGrid = UniformGrid.create(
    GridId("implicit-equivalent:left"),
    GridVersion(1),
    ab,
    PositiveRational.exact(1, 10).toOption.get
  )
  val rightGrid = UniformGrid.create(
    GridId("implicit-equivalent:right"),
    GridVersion(1),
    ba,
    PositiveRational.exact(1, 10).toOption.get
  )
  val leftGridValue = leftGrid.fromCoordinate(2)
  val rightGridValue = rightGrid.fromCoordinate(3)

  val equivalent: SameDimension[AB, BA] = summon
  val alignedRight: Quantity[AB] = right.alignTo[AB]
  val validAdd: Quantity[AB] = left + alignedRight
  val validSubtract: Quantity[AB] = left - alignedRight
  val alignedRightGrid: GridQuantity[AB, rightGrid.G] = rightGridValue.alignTo[AB]

  // OFFENDING-BEGIN
  val invalidAdd = left + right
  val invalidSubtract = left - right
  val invalidGridAdd = leftGridValue.addExact(rightGridValue, leftGrid, rightGrid)
  val invalidGridSubtract = leftGridValue.subtractExact(rightGridValue, leftGrid, rightGrid)
  val invalidAlignedGridAdd = leftGridValue.addExact(alignedRightGrid, leftGrid, rightGrid)
  val invalidAlignedGridSubtract = leftGridValue.subtractExact(alignedRightGrid, leftGrid, rightGrid)
  // OFFENDING-END

end ImplicitEquivalentArithmetic
