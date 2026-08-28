package external.fixtures.positive

import trading.quantity.*
import trading.quantity.refinement.*

object ExplicitEquivalentArithmetic:
  val a = DimRef.atomic(AtomId("explicit-equivalent:a"))
  val b = DimRef.atomic(AtomId("explicit-equivalent:b"))

  type AB = Times[a.D, b.D]
  type BA = Times[b.D, a.D]

  val ab: DimRef[AB] = DimRef.times(a.dimension, b.dimension)
  val ba: DimRef[BA] = DimRef.times(b.dimension, a.dimension)
  val left = Quantity(ab, 2)
  val right = Quantity(ba, 3)
  val alignedRight: Quantity[AB] = right.alignTo[AB]
  val exactAdd: Quantity[AB] = left + alignedRight
  val exactSubtract: Quantity[AB] = left - alignedRight

  val leftGrid = UniformGrid.create(ab,
    PositiveRational.exact(1, 10).toOption.get
  )
  val rightGrid = UniformGrid.create(ba,
    PositiveRational.exact(3, 10).toOption.get
  )
  val leftGridValue = leftGrid.fromCoordinate(2)
  val rightGridValue = rightGrid.fromCoordinate(3)
  val leftGridExact: Quantity[AB] = leftGridValue.asQuantity(leftGrid)
  val rightGridExact: Quantity[BA] = rightGridValue.asQuantity(rightGrid)
  val alignedRightGridExact: Quantity[AB] = rightGridExact.alignTo[AB]
  val gridAdd: Quantity[AB] = leftGridExact + alignedRightGridExact
  val gridSubtract: Quantity[AB] = leftGridExact - alignedRightGridExact

  assert(gridAdd.coefficient == Rational(11, 10))
  assert(gridSubtract.coefficient == Rational(-7, 10))

end ExplicitEquivalentArithmetic
