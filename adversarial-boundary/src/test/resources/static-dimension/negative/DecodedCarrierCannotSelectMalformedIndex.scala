package external.fixtures.negative

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

object DecodedCarrierCannotSelectMalformedIndex:
  type Bad = Canonical[Power["decoded:bad", 0] *: EmptyTuple]

  val registry   = new QuantityRegistry
  val dimension  = registry.registerDimension(DimKey.one).toOption.get
  val definition = GridDefinition(
    dimension.dimension.key,
    GridId("decoded-valid"),
    GridVersion(1),
    PositiveRational.exact(1, 100).toOption.get
  )
  val grid    = registry.registerGrid(dimension)(definition).toOption.get
  val packed  = PackedGridQuantity.pack(grid)(grid.fromCoordinate(1))
  val decoded = PackedGridQuantity.decode(packed, registry).toOption.get

  // OFFENDING-BEGIN
  val malformed: Quantity[Bad] = decoded.grid.asQuantity(decoded.value)
  // OFFENDING-END

end DecodedCarrierCannotSelectMalformedIndex
