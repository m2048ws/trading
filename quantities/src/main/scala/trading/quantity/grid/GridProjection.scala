package trading.quantity.grid

import trading.quantity.*
import trading.quantity.refinement.*

/** Describes an exact quantity that cannot be represented by the requested target grid. */
final case class NotOnGrid[D <: Dimension](source: Rational, target: GridKey, targetQuantum: Rational)
  extends JavaSerializationUnsupported

/** Exact narrowing operations that reject nonrepresentable values instead of rounding them. */
object GridProjection:

  def narrowExactlyTo[D <: Dimension](
    t: GridRef[D]
  )(
    v: Quantity[D]
  ): Either[NotOnGrid[D], GridQuantity[D, t.G]] =
    val coefficient = v.coefficient
    val quantum     = t.quantum.unrefined
    val coordinate  = coefficient.divideBy(t.quantum.asNonZero)

    if coordinate.isWhole then
      Right(t.fromCoordinate(coordinate.numerator / coordinate.denominator))
    else
      Left(NotOnGrid(coefficient, t.key, quantum))

  def narrowGridExactlyTo[D <: Dimension](
    g: GridRef[D],
    t: GridRef[D]
  )(
    v: GridQuantity[D, g.G]
  ): Either[NotOnGrid[D], GridQuantity[D, t.G]] =
    narrowExactlyTo(t)(g.asQuantity(v))

end GridProjection

/** Validation façade for requiring that an exact quantity inhabits a particular grid. */
object GridConstraint:

  def validate[D <: Dimension](
    g: GridRef[D]
  )(
    v: Quantity[D]
  ): Either[NotOnGrid[D], GridQuantity[D, g.G]] =
    GridProjection.narrowExactlyTo(g)(v)

extension [D <: Dimension](v: Quantity[D])
  def narrowExactlyTo(t: GridRef[D]): Either[NotOnGrid[D], GridQuantity[D, t.G]] =
    GridProjection.narrowExactlyTo(t)(v)

extension [D <: Dimension, G](v: GridQuantity[D, G])
  def narrowExactlyTo(
    g: GridRef.Grid[D, G],
    t: GridRef[D]
  ): Either[NotOnGrid[D], GridQuantity[D, t.G]] =
    GridProjection.narrowGridExactlyTo(g, t)(v)
