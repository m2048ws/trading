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
final class Instrument private (val spec: InstrumentSpec) extends JavaSerializationUnsupported:

  val identity: InstrumentIdentity = spec.identity
  val roles: spec.roles.type       = spec.roles

  val positionLotGrid: GridHandle[roles.position.D]              = spec.positionLotGrid
  val priceGrid: GridHandle[Divide[roles.quote.D, roles.base.D]] = spec.priceGrid
  val basePerPosition: Rate[roles.position.D, roles.base.D]      = spec.basePerPosition
  val quotePerPosition: Rate[roles.position.D, roles.quote.D]    = spec.quotePerPosition

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
    val coordinate = positionLotGrid.fromCoordinate(count)
    Positive(coordinate)
      .left
      .map(_ => InvalidLots(count))
      .flatMap: positive =>
        PositiveWhole(positionLotGrid.coordinate(positive.unrefined))
          .left
          .map(_ => InvalidLots(count))
          .map(positiveCount =>
            Lots(identity.id, positiveCount, positionLotGrid.asQuantity(positive.unrefined))
          )

  def positionLots(side: Side, lots: Lots): Either[EconomicsError, PositionLots] =
    IdentityChecks
      .check("positionLots", identity.id, "lots" -> lots.instrumentId)
      .map: _ =>
        val count = side.sign * lots.count.unrefined
        Position(identity.id, count, positionLotGrid.asQuantity(positionLotGrid.fromCoordinate(count)))

  val flatPosition: PositionLots =
    Position(identity.id, BigInt(0), positionLotGrid.asQuantity(positionLotGrid.fromCoordinate(0)))

  val prices: Prices[roles.base.D, roles.quote.D] =
    new Prices(identity.id, roles.base, roles.quote, priceGrid)

  val market: Market[roles.base.D, roles.quote.D, roles.settle.D] =
    new Market(identity.id, roles.base, roles.quote, roles.settle)

  val orders: Orders[roles.position.D, roles.base.D, roles.quote.D] =
    new Orders(identity.id)

  val scenarios: Scenarios[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new Scenarios(identity.id, positionLotGrid)

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
  /** Total construction from the proof-carrying assembly result. */
  def fromSpec(spec: InstrumentSpec): Instrument =
    new Instrument(spec)

end Instrument
