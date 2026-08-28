package external.quantityboundary.positive

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

object QuantitiesOnlyClient:
  def generic[D <: Dim, G](grid: GridRef.Grid[D, G], coordinate: BigInt): Quantity[D] =
    grid.asQuantity(grid.fromCoordinate(coordinate))

  val dimension = DimRef.atomic(AtomId("quantities-only"))
  val cents     = UniformGrid.create(dimension.dimension, PositiveRational.exact(1, 100).toOption.get)
  val exact     = generic(cents, 123)
  val projected = Quantity(dimension.dimension, Rational(123, 100)).narrowExactlyTo(cents)

end QuantitiesOnlyClient
