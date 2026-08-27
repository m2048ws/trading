package trading.quantity

import java.util.Objects

import trading.quantity.GridRef.Grid
import trading.quantity.refinement.*

/**
 * An integer coordinate in grid `G` and dimension `D`.
 *
 * The grid type prevents coordinates from different grids from being mixed, even when the grids share a dimension.
 * Nonzero construction and exact interpretation require the corresponding [[GridRef]], while polymorphic zero requires
 * an authoritative [[DimRef]]. Possessing a value supplies neither a grid witness nor dimension or registry authority.
 */
opaque type GridQuantity[D <: Dim, G] = BigInt

/** Same-grid arithmetic and controlled coordinate access for [[GridQuantity]]. */
object GridQuantity:

  def zero[D <: Dim, G](using dimension: DimRef[D]): GridQuantity[D, G] =
    val _ = dimension.key
    fromCoordinate(BigInt(0))

  private def fromCoordinate[D <: Dim, G](c: BigInt): GridQuantity[D, G] =
    Objects.requireNonNull(c, "grid coordinate")

  private def coordinate[D <: Dim, G](v: GridQuantity[D, G]): BigInt =
    v

  private def add[D <: Dim, G](l: GridQuantity[D, G], r: GridQuantity[D, G]): GridQuantity[D, G] =
    fromCoordinate(l + r)

  private def subtract[D <: Dim, G](l: GridQuantity[D, G], r: GridQuantity[D, G]): GridQuantity[D, G] =
    fromCoordinate(l - r)

  private def scale[D <: Dim, G](v: GridQuantity[D, G], s: BigInt): GridQuantity[D, G] =
    fromCoordinate(v * s)

  /**
   * Type-carrying definition of a grid in dimension `D`.
   *
   * Its path-dependent type member `G` owns a distinct coordinate namespace. The reference is therefore required both
   * to construct a [[GridQuantity]] and to interpret its integer coordinate as an exact [[Quantity]].
   *
   * @tparam D the dimension inhabited by the grid
   */
  sealed trait GridRef[D <: Dim]:
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

  given ordering[D <: Dim, G]: Ordering[GridQuantity[D, G]] with
    def compare(l: GridQuantity[D, G], r: GridQuantity[D, G]): Int = coordinate(l).compare(coordinate(r))

  extension [D <: Dim, G](v: GridQuantity[D, G])

    def +(r: GridQuantity[D, G]): GridQuantity[D, G] =
      add(v, r)

    def -(r: GridQuantity[D, G]): GridQuantity[D, G] =
      subtract(v, r)

    def *(s: BigInt): GridQuantity[D, G] =
      scale(v, s)

    def unary_- : GridQuantity[D, G] =
      scale(v, -1)

    def sameGridEquals(r: GridQuantity[D, G]): Boolean =
      coordinate(v) == coordinate(r)

    def sameGridHash: Int =
      coordinate(v).hashCode

    def compareSameGrid(r: GridQuantity[D, G]): Int =
      coordinate(v).compare(coordinate(r))

    def asQuantity(g: Grid[D, G]): Quantity[D] =
      g.asQuantity(v)

    /** Explicitly align the phantom dimension while preserving the grid identity and coordinate. */
    def alignTo[Target <: Dim](using same: SameDimension[D, Target]): GridQuantity[Target, G] =
      val _ = Objects.requireNonNull(same, "same dimension evidence")
      v.asInstanceOf[GridQuantity[Target, G]]

    def addExact[H](
      r: GridQuantity[D, H],
      lg: Grid[D, G],
      rg: Grid[D, H]
    ): Quantity[D] =
      lg.asQuantity(v) + rg.asQuantity(r)

    def subtractExact[H](
      r: GridQuantity[D, H],
      lg: Grid[D, G],
      rg: Grid[D, H]
    ): Quantity[D] =
      lg.asQuantity(v) - rg.asQuantity(r)

    def exactlyEquals[E <: Dim, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    )(using same: SameDimension[D, E]
    ): Boolean =
      lg.asQuantity(v).alignTo[E].coefficient == rg.asQuantity(r).coefficient

    def compareExact[E <: Dim, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    )(using same: SameDimension[D, E]
    ): Int =
      lg.asQuantity(v).alignTo[E].coefficient.compare(rg.asQuantity(r).coefficient)

    def multiplyExact[E <: Dim, H](
      r: GridQuantity[E, H],
      lg: Grid[D, G],
      rg: Grid[E, H]
    ): Quantity[Times[D, E]] =
      lg.asQuantity(v) * rg.asQuantity(r)

    def multiplyExact[E <: Dim](
      r: Quantity[E],
      g: Grid[D, G]
    ): Quantity[Times[D, E]] =
      g.asQuantity(v) * r

    def applyRate[E <: Dim](
      r: Rate[D, E],
      g: Grid[D, G]
    ): Quantity[E] =
      g.asQuantity(v).applyRate(r)

    def divideBy[E <: Dim](
      d: NonZero[Quantity[E]],
      g: Grid[D, G]
    ): Quantity[Divide[D, E]] =
      g.asQuantity(v).divideBy(d)

    def ratioTo(
      d: NonZero[Quantity[D]],
      g: Grid[D, G]
    ): Ratio =
      g.asQuantity(v).ratioTo(d)

    def exactDivideBy(d: NonZeroWhole, g: Grid[D, G]): Quantity[D] =
      g.asQuantity(v).exactDivideBy(d)

  end extension

end GridQuantity

/** A grid reference whose path-dependent grid identity is retained by the reference value. */
type GridRef[D <: Dim] = GridQuantity.GridRef[D]

/** Type refinements for APIs that need to name a grid reference's associated identity. */
object GridRef:
  /** A grid reference whose associated identity is statically known as `G0`. */
  type Grid[D <: Dim, G0] = GridRef[D] { type G = G0 }

/**
 * Factory for generative, in-memory grid witnesses.
 *
 * Each returned stable value has a fresh associated grid type. Registry-owned provenance is added separately by
 * [[trading.quantity.runtime.QuantityRegistry]].
 */
object UniformGrid:

  def create[D <: Dim](
    gridId: GridId,
    gridVersion: GridVersion,
    dimensionRef: DimRef[D],
    gridQuantum: PositiveRational
  ): GridRef[D] =
    val checkedGridId      = Objects.requireNonNull(gridId, "grid ID")
    val checkedGridVersion = Objects.requireNonNull(gridVersion, "grid version")
    val _                  = dimensionRef.key
    new GridRef[D]:
      type G = this.type
      val id: GridId                = checkedGridId
      val version: GridVersion      = checkedGridVersion
      val dimension: DimRef[D]      = dimensionRef
      val quantum: PositiveRational = gridQuantum
