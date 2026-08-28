package external.fixtures.negative

import trading.quantity.*
import trading.quantity.refinement.*

object UnequalGridComparison:
  val leftDimension = DimRef.atomic(AtomId("unequal-grid-comparison:left"))
  val rightDimension = DimRef.atomic(AtomId("unequal-grid-comparison:right"))
  val leftGrid = UniformGrid.create(leftDimension.dimension,
    PositiveRational.exact(1, 1).toOption.get
  )
  val rightGrid = UniformGrid.create(rightDimension.dimension,
    PositiveRational.exact(1, 1).toOption.get
  )
  val left = leftGrid.fromCoordinate(1)
  val right = rightGrid.fromCoordinate(1)

  // OFFENDING-BEGIN
  val equal = left.exactlyEquals(right, leftGrid, rightGrid)
  val compared = left.compareExact(right, leftGrid, rightGrid)
  // OFFENDING-END

end UnequalGridComparison
