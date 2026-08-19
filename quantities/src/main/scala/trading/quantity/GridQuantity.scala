package trading.quantity

import trading.quantity.GridRef.Grid
import trading.quantity.refinement.*

/**
 * An integer coordinate in grid `G` and dimension `D`.
 *
 * The grid type prevents coordinates from different grids from being mixed, even when the grids share a dimension.
 * Construction and exact interpretation require the corresponding [[GridRef]].
 */
opaque type GridQuantity[D <: Dimension, G] = BigInt

/** Same-grid arithmetic and controlled coordinate access for [[GridQuantity]]. */
object GridQuantity:

  def zero[D <: Dimension, G](using valid: Normalize[D]): GridQuantity[D, G] =
    val _ = valid
    BigInt(0)

  private def fromCoordinate[D <: Dimension, G](c: BigInt): GridQuantity[D, G] =
    c

  private def coordinate[D <: Dimension, G](v: GridQuantity[D, G]): BigInt =
    v

  private def add[D <: Dimension, G](
    l: GridQuantity[D, G],
    r: GridQuantity[D, G]
  )(using
    valid: Normalize[D]
  ): GridQuantity[D, G] =
    val _ = valid
    l + r

  private def subtract[D <: Dimension, G](
    l: GridQuantity[D, G],
    r: GridQuantity[D, G]
  )(using
    valid: Normalize[D]
  ): GridQuantity[D, G] =
    val _ = valid
    l - r

  private def scale[D <: Dimension, G](
    v: GridQuantity[D, G],
    s: BigInt
  )(using
    valid: Normalize[D]
  ): GridQuantity[D, G] =
    val _ = valid
    v * s

  /**
   * Type-carrying definition of a grid in dimension `D`.
   *
   * Its path-dependent type member `G` owns a distinct coordinate namespace. The reference is therefore required both
   * to construct a [[GridQuantity]] and to interpret its integer coordinate as an exact [[Quantity]].
   *
   * @tparam D the dimension inhabited by the grid
   */
  sealed trait GridRef[D <: Dimension]:
    /** Static identity of coordinates created by this grid reference. */
    type G

    def id: GridId
    def version: GridVersion
    def dimension: DimRef[D]
    def quantum: PositiveRational

    final def key: GridKey =
      GridKey(id, version)

    final def fromCoordinate(c: BigInt): GridQuantity[D, G] =
      GridQuantity.fromCoordinate(c)

    final def coordinate(v: GridQuantity[D, G]): BigInt =
      GridQuantity.coordinate(v)

    final def asQuantity(v: GridQuantity[D, G]): Quantity[D] =
      Quantity(dimension, quantum.unrefined * Rational(coordinate(v)))

  end GridRef

  given ordering[D <: Dimension, G]: Ordering[GridQuantity[D, G]] with
    def compare(l: GridQuantity[D, G], r: GridQuantity[D, G]): Int = coordinate(l).compare(coordinate(r))

  extension [D <: Dimension, G](v: GridQuantity[D, G])

    def +(r: GridQuantity[D, G])(using Normalize[D]): GridQuantity[D, G] =
      add(v, r)

    def -(r: GridQuantity[D, G])(using Normalize[D]): GridQuantity[D, G] =
      subtract(v, r)

    def *(s: BigInt)(using Normalize[D]): GridQuantity[D, G] =
      scale(v, s)

    def unary_-(using Normalize[D]): GridQuantity[D, G] =
      scale(v, -1)

    def sameGridEquals(r: GridQuantity[D, G]): Boolean =
      coordinate(v) == coordinate(r)

    def sameGridHash: Int =
      coordinate(v).hashCode

    def compareSameGrid(r: GridQuantity[D, G]): Int =
      coordinate(v).compare(coordinate(r))

    def asQuantity(g: Grid[D, G]): Quantity[D] =
      g.asQuantity(v)

    /** Retag only the phantom dimension while preserving the grid identity and coordinate. */
    def asDimension[Target <: Dimension](using same: SameDimension[D, Target]): GridQuantity[Target, G] =
      same.coerceGrid(v)

    def addExact[E <: Dimension, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    )(using
      Normalize[D],
      Normalize[E],
      SameDimension[D, E]
    ): Quantity[D] =
      lg.asQuantity(v) + rg.asQuantity(r)

    def subtractExact[E <: Dimension, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    )(using
      Normalize[D],
      Normalize[E],
      SameDimension[D, E]
    ): Quantity[D] =
      lg.asQuantity(v) - rg.asQuantity(r)

    def exactlyEquals[E <: Dimension, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    )(using same: SameDimension[D, E]
    ): Boolean =
      same.coerceQuantity(lg.asQuantity(v)).coefficient == rg.asQuantity(r).coefficient

    def compareExact[E <: Dimension, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    )(using same: SameDimension[D, E]
    ): Int =
      same.coerceQuantity(lg.asQuantity(v)).coefficient.compare(rg.asQuantity(r).coefficient)

    def multiplyExact[E <: Dimension, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    )(using
      operation: Normalize[Times[D, E]]
    ): Quantity[operation.Out] =
      lg.asQuantity(v).*(rg.asQuantity(r))(using operation)

    def multiplyExact[E <: Dimension](
      r: Quantity[E],
      g: Grid[D, G]
    )(using
      operation: Normalize[Times[D, E]]
    ): Quantity[operation.Out] =
      g.asQuantity(v).*(r)(using operation)

    def applyRate[E <: Dimension](
      r: Rate[D, E],
      g: Grid[D, G]
    )(using
      operation: Normalize[Times[D, Divide[E, D]]]
    ): Quantity[E] =
      g.asQuantity(v).applyRate(r)(using operation)

    def divideBy[E <: Dimension](
      d: NonZero[Quantity[E]],
      g: Grid[D, G]
    )(using
      operation: Normalize[Divide[D, E]]
    ): Quantity[operation.Out] =
      g.asQuantity(v).divideBy(d)(using operation)

    def ratioTo(
      d: NonZero[Quantity[D]],
      g: Grid[D, G]
    )(using
      operation: Normalize[Divide[D, D]]
    ): Ratio =
      g.asQuantity(v).ratioTo(d)(using operation)

    def exactDivideBy(d: NonZeroWhole, g: Grid[D, G])(using Normalize[D]): Quantity[D] =
      g.asQuantity(v).exactDivideBy(d)

  end extension

end GridQuantity

/** A grid reference whose path-dependent grid identity is retained by the reference value. */
type GridRef[D <: Dimension] = GridQuantity.GridRef[D]

/** Type refinements for APIs that need to name a grid reference's associated identity. */
object GridRef:
  /** A grid reference whose associated identity is statically known as `G0`. */
  type Grid[D <: Dimension, G0] = GridRef[D] { type G = G0 }

/**
 * Factory for generative, in-memory grid witnesses.
 *
 * Each returned stable value has a fresh associated grid type. Registry-owned provenance is added separately by
 * [[trading.quantity.runtime.QuantityRegistry]].
 */
object UniformGrid:

  def create[D <: Dimension](
    gridId: GridId,
    gridVersion: GridVersion,
    dimensionRef: DimRef[D],
    gridQuantum: PositiveRational
  ): GridRef[D] =
    new GridRef[D]:
      type G = this.type
      val id: GridId                = gridId
      val version: GridVersion      = gridVersion
      val dimension: DimRef[D]      = dimensionRef
      val quantum: PositiveRational = gridQuantum
