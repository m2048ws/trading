package trading.economics.instrument

import trading.quantity.*
import trading.reference.*

/** Stable external identity of a validated listing or contract. */
final case class InstrumentId(value: String):
  require(value.trim.nonEmpty, "instrument ID cannot be empty")

/** Stable identity of an instrument's possibly non-currency underlying. */
final case class UnderlyingId(value: String):
  require(value.trim.nonEmpty, "underlying ID cannot be empty")

/** Cohesive semantic identity of one instrument. */
final case class Identity(id: InstrumentId, underlying: UnderlyingId)

/** The trusted asset roles shared by listing rules and payoff terms. */
final class Roles(
  val base: Asset,
  val quote: Asset,
  val position: Asset,
  val settle: Asset)

/** Contextual grids for the exact role value supplied at construction. */
final class ListingRules(
  val roles: Roles
)(
  val positionLotGrid: GridHandle[? <: Dim],
  val priceGrid: GridHandle[? <: Dim])

/** Product-family-neutral two-leg payoff for the exact role value supplied at construction. */
final class ContractPayoff(
  val roles: Roles
)(
  val basePerPosition: Rate[roles.position.D, roles.base.D],
  val quotePerPosition: Rate[roles.position.D, roles.quote.D])

/** One cohesive input to the final validated instrument boundary. */
final case class Definition(
  identity: Identity,
  roles: Roles,
  listingRules: ListingRules,
  contractPayoff: ContractPayoff)
