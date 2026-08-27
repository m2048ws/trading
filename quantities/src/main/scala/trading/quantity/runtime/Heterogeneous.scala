package trading.quantity.runtime

import trading.quantity.*

/** Closed hierarchy of failed operations between values whose types were discovered at runtime. */
sealed abstract class HeterogeneousOperationError extends JavaSerializationUnsupported with Product with Serializable

/** Exact arithmetic could not establish compatible runtime dimensions. */
final case class HeterogeneousDimensionMismatch(cause: RuntimeEvidenceError) extends HeterogeneousOperationError

/** Same-grid arithmetic could not establish compatible runtime grid identities. */
final case class HeterogeneousGridMismatch(cause: RuntimeEvidenceError) extends HeterogeneousOperationError

/**
 * An exact quantity whose dimension was established at runtime.
 *
 * The dependent value keeps the resolved witness and its quantity together, preserving dimensional safety after a
 * heterogeneous operation.
 */
sealed trait ResolvedExactQuantity extends JavaSerializationUnsupported:
  type D <: Dim
  def dimension: DimRef[D]
  def value: Quantity[D]

private final class ResolvedExactQuantityImpl[D0 <: Dim](
  val dimension: DimRef[D0],
  val value: Quantity[D0])
  extends ResolvedExactQuantity:
  type D = D0

private object ResolvedExactQuantity:
  def apply[D <: Dim](dimension: DimRef[D], value: Quantity[D]): ResolvedExactQuantity =
    new ResolvedExactQuantityImpl(dimension, value)

/**
 * Type-safe operations on quantities whose dimensions and grids are known only through runtime witnesses.
 *
 * Each operation recovers the necessary equality evidence before combining values and returns a dependent result that
 * retains the resolved identities.
 */
object HeterogeneousQuantity:

  def generalize(v: ResolvedAssetGridQuantity): ResolvedGridQuantity =
    new ResolvedGridQuantity(v.asset)(v.grid)(v.value)

  def addSameGrid(
    l: ResolvedGridQuantity,
    r: ResolvedGridQuantity
  ): Either[HeterogeneousOperationError, ResolvedGridQuantity] =
    RuntimeEvidence
      .sameGrid(r.grid, l.grid)
      .left
      .map(HeterogeneousGridMismatch(_))
      .map: e =>
        val retyped: GridQuantity[l.dimension.D, l.grid.G] = e.retype(r.value)
        new ResolvedGridQuantity(l.dimension)(l.grid)(l.value + retyped)

  def addExact(
    l: ResolvedGridQuantity,
    r: ResolvedGridQuantity
  ): Either[HeterogeneousOperationError, ResolvedExactQuantity] =
    RuntimeEvidence
      .sameDimension(r.dimension, l.dimension)
      .left
      .map(HeterogeneousDimensionMismatch(_))
      .map: e =>
        val aligned: Quantity[l.dimension.D] = r.grid.asQuantity(r.value).alignTo[l.dimension.D](using e)
        val result                           = l.grid.asQuantity(l.value) + aligned
        ResolvedExactQuantity(l.dimension.dimension.ref, result)

  /** Multiply heterogeneous trusted values while retaining the raw expression witness and exact coefficient. */
  def multiplyExact(l: ResolvedGridQuantity, r: ResolvedGridQuantity): ResolvedExactQuantity =
    val left      = l.grid.asQuantity(l.value)
    val right     = r.grid.asQuantity(r.value)
    val dimension = DimRef.times(l.dimension.dimension.ref, r.dimension.dimension.ref)
    ResolvedExactQuantity(dimension, left * right)

end HeterogeneousQuantity
