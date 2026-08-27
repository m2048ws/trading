package trading.quantity.grid

import trading.quantity.*

/**
 * Dimension-local encoding of an exact quantity as a grid key and integer coordinate.
 *
 * A grid key is scoped by its owning dimension, so that dimension must be supplied by the surrounding boundary.
 */
final case class GridCoordinateEncoding(localGridKey: GridKey, coordinate: BigInt) extends JavaSerializationUnsupported

/** Encodes exact quantities only when they are representable on the requested grid without rounding. */
object ConstrainedGridEncoding:
  def encodeExact[D <: Dim](
    g: GridRef[D]
  )(
    v: Quantity[D]
  ): Either[NotOnGrid[D], GridCoordinateEncoding] =
    GridConstraint
      .validate(g)(v)
      .map(q => GridCoordinateEncoding(g.key, g.coordinate(q)))
