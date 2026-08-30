package trading.economics.instrument

import java.util.Objects

import trading.quantity.*
import trading.reference.*

/** Expected failures while constructing stable instrument identity values. */
sealed abstract class InstrumentIdentityError extends JavaSerializationUnsupported with Product with Serializable

case object EmptyInstrumentId extends InstrumentIdentityError
case object EmptyUnderlyingId extends InstrumentIdentityError

/** Stable external identity of a listing or contract. */
final class InstrumentId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean =
    other match
      case that: InstrumentId => value == that.value
      case _                  => false

  override def hashCode: Int    = value.hashCode
  override def toString: String = s"InstrumentId($value)"
end InstrumentId

object InstrumentId:
  def from(value: String): Either[EmptyInstrumentId.type, InstrumentId] =
    val checked = Objects.requireNonNull(value, "instrument ID")
    if checked.trim.nonEmpty then Right(new InstrumentId(checked)) else Left(EmptyInstrumentId)
end InstrumentId

/** Stable identity of an instrument's possibly non-currency underlying. */
final class UnderlyingId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean =
    other match
      case that: UnderlyingId => value == that.value
      case _                  => false

  override def hashCode: Int    = value.hashCode
  override def toString: String = s"UnderlyingId($value)"
end UnderlyingId

object UnderlyingId:
  def from(value: String): Either[EmptyUnderlyingId.type, UnderlyingId] =
    val checked = Objects.requireNonNull(value, "underlying ID")
    if checked.trim.nonEmpty then Right(new UnderlyingId(checked)) else Left(EmptyUnderlyingId)
end UnderlyingId

/** Cohesive semantic identity of one instrument. */
final case class InstrumentIdentity(id: InstrumentId, underlying: UnderlyingId) extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(id, "instrument ID")
  val _ = Objects.requireNonNull(underlying, "underlying ID")

/** Closed asset fields resolved by instrument assembly. */
enum AssetRole extends JavaSerializationUnsupported:
  case Base, Quote, Position, Settle

/** Closed listing-grid fields resolved by instrument assembly. */
enum ListingGridRole extends JavaSerializationUnsupported:
  case PositionLot, Price

/** Stable asset identities for every economic role. */
final case class AssetRoleIds(
  base: AssetId,
  quote: AssetId,
  position: AssetId,
  settle: AssetId)
  extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(base, "base asset ID")
  val _ = Objects.requireNonNull(quote, "quote asset ID")
  val _ = Objects.requireNonNull(position, "position asset ID")
  val _ = Objects.requireNonNull(settle, "settle asset ID")

/** Full stable identities of the position-lot and quote-per-base price grids. */
final case class ListingDefinition(
  positionLotGrid: GridIdentity,
  priceGrid: GridIdentity)
  extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(positionLotGrid, "position-lot grid identity")
  val _ = Objects.requireNonNull(priceGrid, "price grid identity")

/** Exact product-family-neutral two-leg payoff coefficients before endpoint resolution. */
final case class PayoffDefinition(
  basePerPosition: Rational,
  quotePerPosition: Rational)
  extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(basePerPosition, "base-per-position coefficient")
  val _ = Objects.requireNonNull(quotePerPosition, "quote-per-position coefficient")

/** Stable-ID-only in-memory command consumed by [[InstrumentAssembler]]. */
final case class InstrumentDefinition(
  identity: InstrumentIdentity,
  roles: AssetRoleIds,
  listing: ListingDefinition,
  payoff: PayoffDefinition)
  extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(identity, "instrument identity")
  val _ = Objects.requireNonNull(roles, "asset role IDs")
  val _ = Objects.requireNonNull(listing, "listing definition")
  val _ = Objects.requireNonNull(payoff, "payoff definition")
