package trading.quantity.grid

import trading.quantity.*
import trading.quantity.refinement.*

/**
 * Result of projecting an exact quantity onto grid `G`.
 *
 * `value` is the selected grid coordinate and `residual` is the exact difference between the source quantity and that
 * coordinate's represented value. The chosen [[QuantizationPolicy]] determines their rounding relationship.
 *
 * @tparam D the quantity dimension
 * @tparam G the target grid identity
 */
final class Quantization[D <: Dimension, G] private (val value: GridQuantity[D, G], val residual: Quantity[D])
  extends JavaSerializationUnsupported

/** Projects exact or grid quantities onto a target grid using an explicit [[QuantizationPolicy]]. */
object Quantization:

  def toGrid[D <: Dimension](
    t: GridRef[D]
  )(
    v: Quantity[D],
    p: QuantizationPolicy
  )(using Normalize[D]
  ): Quantization[D, t.G] =
    val exactCoordinate    = v.coefficient.divideBy(t.quantum.asNonZero)
    val selectedCoordinate = p.roundCoordinate(exactCoordinate)

    require(
      p.acceptsResidual(exactCoordinate, selectedCoordinate),
      "quantization policy violated its documented residual bound"
    )

    val selected = t.fromCoordinate(selectedCoordinate)
    val residual = v - t.asQuantity(selected)

    new Quantization(selected, residual)

  end toGrid

  def gridToGrid[D <: Dimension](
    g: GridRef[D],
    t: GridRef[D]
  )(
    v: GridQuantity[D, g.G],
    p: QuantizationPolicy
  )(using Normalize[D]
  ): Quantization[D, t.G] =
    toGrid(t)(g.asQuantity(v), p)

end Quantization

extension [D <: Dimension](v: Quantity[D])

  def quantizeTo(t: GridRef[D], p: QuantizationPolicy)(using Normalize[D]): Quantization[D, t.G] =
    Quantization.toGrid(t)(v, p)

extension [D <: Dimension, G](v: GridQuantity[D, G])

  def quantizeTo(
    g: GridRef.Grid[D, G],
    t: GridRef[D],
    p: QuantizationPolicy
  )(using Normalize[D]
  ): Quantization[D, t.G] =
    Quantization.gridToGrid(g, t)(v, p)
