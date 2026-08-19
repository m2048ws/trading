package external.fixtures.negative

import trading.quantity.*
import trading.quantity.grid.*

object MalformedGridBoundaryArithmetic:
  type Bad = Dim[Power["grid-boundary:bad", 0] *: EmptyTuple]
  sealed trait G
  sealed trait H

  // OFFENDING-BEGIN
  def quantizeQuantity(value: Quantity[Bad], target: GridRef[Bad]) =
    value.quantizeTo(target, QuantizationPolicy.HalfEven)

  def quantizeGrid(value: GridQuantity[Bad, G], source: GridRef.Grid[Bad, G], target: GridRef[Bad]) =
    value.quantizeTo(source, target, QuantizationPolicy.HalfEven)

  def project(value: Quantity[Bad], target: GridRef[Bad]) =
    value.narrowExactlyTo(target)

  def constrain(value: Quantity[Bad], target: GridRef[Bad]) =
    GridConstraint.validate(target)(value)

  def encode(value: Quantity[Bad], target: GridRef[Bad]) =
    ConstrainedGridEncoding.encodeExact(target)(value)

  def embed(value: GridQuantity[Bad, G], embedding: Embedding[Bad, G, Bad, H]) =
    embedding.widenTo(value)
  // OFFENDING-END

end MalformedGridBoundaryArithmetic
