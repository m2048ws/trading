package trading.risk

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

/** Public checked input for one inclusive affine interval of a piecewise loss curve. */
final case class LossSegment[S <: Dim](
  start: BigInt,
  end: BigInt,
  startLoss: Quantity[S],
  additionalLotLoss: Quantity[S])
  extends JavaSerializationUnsupported

/** Quantization policies whose monotonicity is already established by the uniform-grid quantity laws. */
enum OrderPreservingQuantization:
  case Floor, Ceiling

  private[risk] def policy: QuantizationPolicy =
    this match
      case Floor   => QuantizationPolicy.Floor
      case Ceiling => QuantizationPolicy.Ceiling
end OrderPreservingQuantization

/** Exact structural work retained when a monotone model is constructed. */
final case class CurveConstructionCost(
  expressionNodes: BigInt,
  explicitBreakpoints: BigInt,
  tableRowsInspected: BigInt)
  extends JavaSerializationUnsupported:

  private[risk] def combine(other: CurveConstructionCost): CurveConstructionCost =
    CurveConstructionCost(
      expressionNodes + other.expressionNodes + 1,
      explicitBreakpoints + other.explicitBreakpoints,
      tableRowsInspected + other.tableRowsInspected
    )
end CurveConstructionCost

private final case class NormalizedLossSegment[S <: Dim](
  start: BigInt,
  end: BigInt,
  startLoss: Quantity[S],
  additionalLotLoss: NonNegative[Quantity[S]]):

  def lossAt(coordinate: BigInt): Quantity[S] =
    startLoss + additionalLotLoss.unrefined * Rational(coordinate - start)
end NormalizedLossSegment

/** Closed exact signed-loss representation; successful public construction is its only admission path. */
private sealed abstract class LotLossFormula[S <: Dim]:
  def lossAt(count: PositiveWhole): Quantity[S]

private object LotLossFormula:
  def quantized[S <: Dim](
    source: LotLossFormula[S]
  )(
    grid: GridRef[S]
  )(
    policy: OrderPreservingQuantization
  ): LotLossFormula[S] =
    QuantizedLossFormula(source, grid, policy)

private final case class AffineLossFormula[S <: Dim](
  firstLotLoss: Quantity[S],
  additionalLotLoss: NonNegative[Quantity[S]])
  extends LotLossFormula[S]:

  def lossAt(count: PositiveWhole): Quantity[S] =
    firstLotLoss + additionalLotLoss.unrefined * Rational(count.unrefined - 1)
end AffineLossFormula

private final case class PiecewiseLossFormula[S <: Dim](segments: Vector[NormalizedLossSegment[S]])
  extends LotLossFormula[S]:

  def lossAt(count: PositiveWhole): Quantity[S] =
    val coordinate = count.unrefined

    @scala.annotation.tailrec
    def locate(low: Int, high: Int): NormalizedLossSegment[S] =
      val middle  = low + (high - low) / 2
      val segment = segments(middle)
      if coordinate < segment.start then locate(low, middle - 1)
      else if coordinate > segment.end then locate(middle + 1, high)
      else segment

    locate(0, segments.size - 1).lossAt(coordinate)
  end lossAt
end PiecewiseLossFormula

private final case class AddedLossFormula[S <: Dim](left: LotLossFormula[S], right: LotLossFormula[S])
  extends LotLossFormula[S]:
  def lossAt(count: PositiveWhole): Quantity[S] = left.lossAt(count) + right.lossAt(count)

private final case class MinimumLossFormula[S <: Dim](left: LotLossFormula[S], right: LotLossFormula[S])
  extends LotLossFormula[S]:
  def lossAt(count: PositiveWhole): Quantity[S] =
    val first  = left.lossAt(count)
    val second = right.lossAt(count)
    if first.coefficient.compare(second.coefficient) <= 0 then first else second

private final case class MaximumLossFormula[S <: Dim](left: LotLossFormula[S], right: LotLossFormula[S])
  extends LotLossFormula[S]:
  def lossAt(count: PositiveWhole): Quantity[S] =
    val first  = left.lossAt(count)
    val second = right.lossAt(count)
    if first.coefficient.compare(second.coefficient) >= 0 then first else second

private final case class QuantizedLossFormula[S <: Dim](
  source: LotLossFormula[S],
  grid: GridRef[S],
  quantization: OrderPreservingQuantization)
  extends LotLossFormula[S]:

  def lossAt(count: PositiveWhole): Quantity[S] =
    val quantized = source.lossAt(count).quantizeTo(grid, quantization.policy)
    grid.asQuantity(quantized.value)
end QuantizedLossFormula

private final case class TableLossFormula[S <: Dim](losses: Vector[Quantity[S]]) extends LotLossFormula[S]:
  def lossAt(count: PositiveWhole): Quantity[S] = losses(count.unrefined.toInt - 1)
