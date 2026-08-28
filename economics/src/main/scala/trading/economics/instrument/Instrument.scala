package trading.economics.instrument

import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

/** Strictly positive lots for one ordinary runtime instrument identity. */
final case class Lots[D <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  count: PositiveWhole,
  quantity: Quantity[D])

/** Signed position lots for one ordinary runtime instrument identity, including flat zero. */
final case class Position[D <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  count: BigInt,
  quantity: Quantity[D])

/** One validated instrument aggregate. Runtime identity is ordinary domain data, not issuance authority. */
final class Instrument private (
  val identity: Identity,
  val roles: Roles,
  val listingRules: ListingRules,
  val contractPayoff: ContractPayoff
)(
  private val positionGrid: GridHandle[roles.position.D],
  private val priceGrid: GridHandle[Divide[roles.quote.D, roles.base.D]],
  private val basePerPosition: Rate[roles.position.D, roles.base.D],
  private val quotePerPosition: Rate[roles.position.D, roles.quote.D]):

  type Lots              = _root_.trading.economics.instrument.Lots[roles.position.D]
  type PositionLots      = _root_.trading.economics.instrument.Position[roles.position.D]
  type Price             = _root_.trading.economics.instrument.Price[roles.base.D, roles.quote.D]
  type MarketState       = _root_.trading.economics.instrument.MarketState[roles.base.D, roles.quote.D, roles.settle.D]
  type Order             = _root_.trading.economics.instrument.Order[Lots, Price]
  type OrderScenario     = _root_.trading.economics.instrument.OrderScenario[Lots, Price, MarketState, PositionLots]
  type RoundTripScenario = _root_.trading.economics.instrument.RoundTripScenario[Lots, Price, MarketState, PositionLots]
  type Fee               = _root_.trading.economics.instrument.Fee[? <: Dim]
  type FeeLine           = _root_.trading.economics.instrument.FeeLine[? <: Dim, MarketState]
  type FeeSchedule       = _root_.trading.economics.instrument.FeeSchedule[Lots, Price, MarketState, PositionLots]
  type Pnl               = _root_.trading.economics.instrument.Pnl[roles.settle.D]

  def lots(count: BigInt): Either[EconomicsError, Lots] =
    val coordinate = positionGrid.fromCoordinate(count)
    Positive(coordinate)
      .left
      .map(_ => InvalidLots(count))
      .map: positive =>
        Lots(
          identity.id,
          PositiveWhole(positionGrid.coordinate(positive.unrefined)).toOption.get,
          positionGrid.asQuantity(positive.unrefined)
        )

  def positionLots(side: Side, lots: Lots): Either[EconomicsError, PositionLots] =
    IdentityChecks
      .check("positionLots", identity.id, "lots" -> lots.instrumentId)
      .map: _ =>
        val count = side.sign * lots.count.unrefined
        Position(identity.id, count, positionGrid.asQuantity(positionGrid.fromCoordinate(count)))

  val flatPosition: PositionLots =
    Position(identity.id, BigInt(0), positionGrid.asQuantity(positionGrid.fromCoordinate(0)))

  val prices: Prices[roles.base.D, roles.quote.D] =
    new Prices(identity.id, roles.base, roles.quote, priceGrid)

  val market: Market[roles.base.D, roles.quote.D, roles.settle.D] =
    new Market(identity.id, roles.base, roles.quote, roles.settle)

  val orders: Orders[roles.position.D, roles.base.D, roles.quote.D] =
    new Orders(identity.id)

  val scenarios: Scenarios[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new Scenarios(identity.id, positionGrid)

  val fees: Fees[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new Fees(identity.id, roles.settle)

  val valuation: Valuation[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new Valuation(
      identity.id,
      roles.settle,
      basePerPosition,
      quotePerPosition
    )
  val sizing: Sizing[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new Sizing(identity.id, roles.settle.dimension.ref, lots, valuation)

end Instrument

object Instrument:
  /** Accumulating raw-definition validation with deterministic, domain-owned diagnostics. */
  def validate(definition: Definition): Either[InvalidDefinition, ValidatedDefinition] =
    ValidatedDefinition.validate(definition)

  /** Total construction from proof-carrying definition evidence. */
  def fromValidated(validated: ValidatedDefinition): Instrument =
    val raw: validated.raw.type = validated.raw
    new Instrument(raw.identity, raw.roles, raw.listingRules, raw.contractPayoff)(
      validated.positionGrid,
      validated.priceGrid,
      validated.basePerPosition,
      validated.quotePerPosition
    )

  /** Final validated definition boundary. */
  def create(definition: Definition): Either[EconomicsError, Instrument] =
    validate(definition)
      .left
      .map(error => ViolationMapping.definition(error.head))
      .map(fromValidated)
  end create

end Instrument
