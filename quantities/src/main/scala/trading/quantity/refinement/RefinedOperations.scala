package trading.quantity.refinement

import scala.annotation.targetName

import trading.quantity.*
import trading.quantity.grid.Quantization
import trading.quantity.grid.QuantizationPolicy

/** Quotient and remainder whose two result components retain their independently proven refinements. */
final case class RefinedQuotRem[Q, R](quotient: Q, remainder: R) extends JavaSerializationUnsupported

/** Quantized grid value that retains refinement evidence together with its unrestricted exact residual. */
final case class RefinedQuantization[Q, D <: Dimension](value: Q, residual: Quantity[D])
  extends JavaSerializationUnsupported

private def checkedNonNegativeResult[A](v: A)(using Sign[A]): NonNegative[A] =
  NonNegative(v).fold(_ => throw new IllegalStateException("refined operation violated nonnegative closure"), identity)

extension [D <: Dimension, G](v: NonNegative[GridQuantity[D, G]])

  @targetName("subtractNonNegativeGrid")
  def subtract(r: NonNegative[GridQuantity[D, G]]): GridQuantity[D, G] =
    NonNegative.unrefined(v) - NonNegative.unrefined(r)

  @targetName("subtractCheckedNonNegativeGrid")
  def subtractChecked(
    r: NonNegative[GridQuantity[D, G]]
  ): Either[ExpectedNonNegative.type, NonNegative[GridQuantity[D, G]]] =
    NonNegative(NonNegative.unrefined(v) - NonNegative.unrefined(r))

  @targetName("quantizeNonNegativeGrid")
  def quantizeTo(
    g: GridRef.Grid[D, G],
    t: GridRef[D],
    p: QuantizationPolicy
  ): RefinedQuantization[NonNegative[GridQuantity[D, t.G]], D] =
    val q = Quantization.gridToGrid(g, t)(NonNegative.unrefined(v), p)
    RefinedQuantization(checkedNonNegativeResult(q.value), q.residual)

end extension

extension [D <: Dimension](v: NonNegative[Quantity[D]])

  @targetName("subtractNonNegativeQuantity")
  def subtract(r: NonNegative[Quantity[D]]): Quantity[D] =
    NonNegative.unrefined(v) - NonNegative.unrefined(r)

  @targetName("subtractCheckedNonNegativeQuantity")
  def subtractChecked(
    r: NonNegative[Quantity[D]]
  ): Either[ExpectedNonNegative.type, NonNegative[Quantity[D]]] =
    NonNegative(NonNegative.unrefined(v) - NonNegative.unrefined(r))

  @targetName("quantizeNonNegativeQuantity")
  def quantizeTo(
    t: GridRef[D],
    p: QuantizationPolicy
  ): RefinedQuantization[NonNegative[GridQuantity[D, t.G]], D] =
    val q = Quantization.toGrid(t)(NonNegative.unrefined(v), p)
    RefinedQuantization(checkedNonNegativeResult(q.value), q.residual)

end extension

extension [D <: Dimension, G](v: Positive[GridQuantity[D, G]])

  @targetName("subtractPositiveGrid")
  def subtract(r: Positive[GridQuantity[D, G]]): GridQuantity[D, G] =
    Positive.unrefined(v) - Positive.unrefined(r)

  @targetName("quantizePositiveGrid")
  def quantizeTo(
    g: GridRef.Grid[D, G],
    t: GridRef[D],
    p: QuantizationPolicy
  ): RefinedQuantization[NonNegative[GridQuantity[D, t.G]], D] =
    val q = Quantization.gridToGrid(g, t)(Positive.unrefined(v), p)
    RefinedQuantization(checkedNonNegativeResult(q.value), q.residual)

end extension

extension [D <: Dimension](v: Positive[Quantity[D]])

  @targetName("subtractPositiveQuantity")
  def subtract(r: Positive[Quantity[D]]): Quantity[D] =
    Positive.unrefined(v) - Positive.unrefined(r)

  @targetName("quantizePositiveQuantity")
  def quantizeTo(
    t: GridRef[D],
    p: QuantizationPolicy
  ): RefinedQuantization[NonNegative[GridQuantity[D, t.G]], D] =
    val q = Quantization.toGrid(t)(Positive.unrefined(v), p)
    RefinedQuantization(checkedNonNegativeResult(q.value), q.residual)

end extension

extension [D <: Dimension, G](v: NonZero[GridQuantity[D, G]])

  @targetName("quantizeNonZeroGrid")
  def quantizeTo(
    g: GridRef.Grid[D, G],
    t: GridRef[D],
    p: QuantizationPolicy
  ): Quantization[D, t.G] =
    Quantization.gridToGrid(g, t)(NonZero.unrefined(v), p)

end extension

extension [D <: Dimension](v: NonZero[Quantity[D]])

  @targetName("quantizeNonZeroQuantity")
  def quantizeTo(t: GridRef[D], p: QuantizationPolicy): Quantization[D, t.G] =
    Quantization.toGrid(t)(NonZero.unrefined(v), p)

end extension
