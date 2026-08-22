package trading.quantity.grid

import trading.quantity.*
import trading.quantity.refinement.*

/** Quotient and remainder produced by Euclidean division of a grid coordinate. */
final case class QuotRem[Q](quotient: Q, remainder: Q) extends JavaSerializationUnsupported

/** Selects which allocation positions receive indivisible remainder units. */
enum RemainderOrder:
  /** Assign remainder units from the first allocation position forward. */
  case FirstToLast

  /** Assign remainder units from the last allocation position backward. */
  case LastToFirst

  @throws[java.io.ObjectStreamException]
  protected final def writeReplace(): AnyRef = throw JavaSerializationUnsupported.failure(this)

  @throws[java.io.ObjectStreamException]
  protected final def readResolve(): AnyRef = throw JavaSerializationUnsupported.failure(this)

  def indices(count: PositiveInt): IndexedSeq[Int] =
    this match
      case FirstToLast => 0.until(count.unrefined)
      case LastToFirst => 0.until(count.unrefined).reverse

/**
 * A non-empty, ordered collection of allocated parts.
 *
 * Allocations produced by grid operations preserve the original coordinate exactly; the order records how indivisible
 * remainder units were distributed.
 *
 * @tparam Q the allocated quantity type
 */
final class Allocation[Q] private (val parts: Vector[Q]) extends JavaSerializationUnsupported:
  def size: Int = parts.size

/** Validated constructors for non-empty allocations. */
object Allocation:

  def fromParts[Q](parts: Vector[Q]): Either[ExpectedPositive.type, Allocation[Q]] =
    if parts.nonEmpty then
      Right(new Allocation(parts))
    else
      Left(ExpectedPositive)

  def tabulate[Q](count: PositiveInt)(f: Int => Q): Allocation[Q] =
    new Allocation(Vector.tabulate(count.unrefined)(f))

extension [D <: Dimension, G](v: GridQuantity[D, G])

  /** Euclidean coordinate division using the matching grid witness for safe inspection and reconstruction. */
  def quotRemBy(d: PositiveWhole, g: GridRef.Grid[D, G]): QuotRem[GridQuantity[D, G]] =
    val (truncatedQuotient, truncatedRemainder) = g.coordinate(v) /% d.unrefined

    val (quotient, remainder) =
      if truncatedRemainder < 0 then
        (truncatedQuotient - 1, truncatedRemainder + d.unrefined)
      else
        (truncatedQuotient, truncatedRemainder)

    QuotRem(g.fromCoordinate(quotient), g.fromCoordinate(remainder))

  /** Ordered allocation using only witness-owned coordinate inspection and construction. */
  def allocateEvenly(
    n: PositiveInt,
    o: RemainderOrder,
    g: GridRef.Grid[D, G]
  ): Allocation[GridQuantity[D, G]] =
    val split           = v.quotRemBy(n.toPositiveWhole, g)
    val baseCoordinate  = g.coordinate(split.quotient)
    val extraRecipients = o.indices(n).take(g.coordinate(split.remainder).toInt).toSet

    Allocation.tabulate(n): index =>
      g.fromCoordinate(baseCoordinate + (if extraRecipients.contains(index) then 1 else 0))

end extension
