package trading.quantity.grid

import trading.quantity.*
import trading.quantity.refinement.*

/** Failures encountered while establishing compatibility between grid references. */
sealed abstract class GridError extends JavaSerializationUnsupported with Product with Serializable

/** The compared grids belong to different canonical dimensions. */
case object GridDimensionMismatch extends GridError

/** The compared grids have different identifiers or versions. */
case object GridIdentityMismatch extends GridError

/** The compared grids represent different exact values per coordinate unit. */
case object GridQuantumMismatch extends GridError

/** One grid identity was paired with conflicting immutable definitions. */
case object GridDefinitionConflict extends GridError

/** The source grid has values that cannot all be represented exactly on the target grid. */
case object NoGridEmbedding extends GridError

/**
 * Evidence that two references describe the same grid identity and immutable definition.
 *
 * Once obtained, the evidence can safely retype a coordinate from the first reference for use with the second.
 */
final class SameGrid[A <: Dimension, G, B <: Dimension, H] private ():

  def retype(v: GridQuantity[A, G]): GridQuantity[B, H] =
    v.asInstanceOf[GridQuantity[B, H]]

/** Validates two grid definitions and recovers [[SameGrid]] evidence when their identities agree. */
object SameGrid:

  def between[A <: Dimension, B <: Dimension](
    l: GridRef[A],
    r: GridRef[B]
  ): Either[GridError, SameGrid[A, l.G, B, r.G]] =
    if l.dimension.key != r.dimension.key then
      Left(GridDimensionMismatch)
    else if l.id != r.id || l.version != r.version then
      Left(GridIdentityMismatch)
    else if l.quantum.unrefined != r.quantum.unrefined then
      Left(GridDefinitionConflict)
    else
      Right(new SameGrid())

/**
 * Evidence that two grids share a dimension and exact quantum.
 *
 * This permits numerical coordinate conversion without claiming that the grids have the same identity.
 */
final class SameQuantum[A <: Dimension, G, B <: Dimension, H] private ():

  /** Numerical conversion only; this does not confer target-grid identity or external validity. */
  def convert(v: GridQuantity[A, G]): GridQuantity[B, H] =
    v.asInstanceOf[GridQuantity[B, H]]

/** Recovers [[SameQuantum]] evidence without treating distinct grid identities as interchangeable. */
object SameQuantum:

  def between[A <: Dimension, B <: Dimension](
    l: GridRef[A],
    r: GridRef[B]
  ): Either[GridError, SameQuantum[A, l.G, B, r.G]] =
    if l.dimension.key != r.dimension.key then
      Left(GridDimensionMismatch)
    else if l.quantum.unrefined != r.quantum.unrefined then
      Left(GridQuantumMismatch)
    else
      Right(new SameQuantum())

/**
 * Exact, global embedding of one grid into another in the same dimension.
 *
 * An embedding exists when every source coordinate is representable on the target grid. `coordinateFactor` scales a
 * source coordinate into its target coordinate without rounding.
 */
final class Embedding[A <: Dimension, G, B <: Dimension, H] private (
  val coordinateFactor: BigInt,
  s: GridRef.Grid[A, G],
  t: GridRef.Grid[B, H]):

  def widenTo(v: GridQuantity[A, G]): GridQuantity[B, H] =
    t.fromCoordinate(s.coordinate(v) * coordinateFactor)

/** Computes exact whole-coordinate embeddings between compatible grid definitions. */
object Embedding:

  def between[A <: Dimension, B <: Dimension](
    f: GridRef[A],
    t: GridRef[B]
  ): Either[GridError, Embedding[A, f.G, B, t.G]] =
    if f.dimension.key != t.dimension.key then
      Left(GridDimensionMismatch)
    else
      val ratio = f.quantum.unrefined.divideBy(t.quantum.asNonZero)

      if ratio.isWhole then
        Right(new Embedding(ratio.numerator / ratio.denominator, f, t))
      else
        Left(NoGridEmbedding)

end Embedding
