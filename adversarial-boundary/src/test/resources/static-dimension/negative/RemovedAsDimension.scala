package external.fixtures.negative

import trading.quantity.*

object RemovedAsDimension:
  val source = DimRef.atomic(AtomId("removed-as-dimension:source"))
  val target = DimRef.atomic(AtomId("removed-as-dimension:source"))
  val exact = Quantity(source.dimension, 1)
  val grid = UniformGrid.create(source.dimension,
    trading.quantity.refinement.PositiveRational.exact(1, 1).toOption.get
  )
  val gridValue = grid.fromCoordinate(1)
  val runtimeAccessor: DimRef[source.D] = source.dimension

  // OFFENDING-BEGIN
  val exactOldName = exact.asDimension[target.D]
  val gridOldName = gridValue.asDimension[target.D]
  // OFFENDING-END

end RemovedAsDimension
