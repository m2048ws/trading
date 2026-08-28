package external.fixtures.negative

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

object RemovedExplicitNormalizationArguments:
  val authority: DimRef[One]         = DimRef.one
  val left: Quantity[One]           = Quantity(DimRef.one, 1)
  val right: Quantity[One]          = Quantity(DimRef.one, 2)
  val grid                          = UniformGrid.create(DimRef.one,
    PositiveRational.exact(1, 100).toOption.get
  )
  val coordinate = grid.fromCoordinate(1)

  // OFFENDING-BEGIN
  val sum       = left.+(right)(using authority)
  val scaled    = left.*(Rational(2))(using authority)
  val gridSum   = coordinate.+(coordinate)(using authority)
  val projected = left.narrowExactlyTo(grid)(using authority)
  // OFFENDING-END

end RemovedExplicitNormalizationArguments
