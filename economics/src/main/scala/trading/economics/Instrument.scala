package trading.economics

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

/** Strictly positive lots for one ordinary runtime instrument identity. */
final case class InstrumentLots[D <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  count: PositiveWhole,
  quantity: Quantity[D])

/** Signed position lots for one ordinary runtime instrument identity, including flat zero. */
final case class InstrumentPosition[D <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  count: BigInt,
  quantity: Quantity[D])

/** One validated instrument aggregate. Runtime identity is ordinary domain data, not issuance authority. */
final class Instrument private (
  val identity: InstrumentIdentity,
  val roles: InstrumentRoles,
  val listingRules: ListingRules,
  val contractPayoff: ContractPayoff
)(
  private val positionGrid: RegisteredGridRef[roles.position.D],
  private val priceGrid: RegisteredGridRef[Divide[roles.quote.D, roles.base.D]]):

  type Lots              = InstrumentLots[roles.position.D]
  type PositionLots      = InstrumentPosition[roles.position.D]
  type Price             = InstrumentPrice[roles.base.D, roles.quote.D]
  type MarketState       = InstrumentMarketState[roles.base.D, roles.quote.D, roles.settle.D]
  type Order             = InstrumentOrder[Lots, Price]
  type OrderScenario     = InstrumentOrderScenario[Lots, Price, MarketState, PositionLots]
  type RoundTripScenario = InstrumentRoundTripScenario[Lots, Price, MarketState, PositionLots]
  type Fee               = InstrumentFee[? <: Dimension]
  type FeeLine           = InstrumentFeeLine[? <: Dimension, MarketState]
  type FeeSchedule       = InstrumentFeeSchedule[Lots, Price, MarketState, PositionLots]
  type Pnl               = InstrumentPnl[roles.settle.D]

  def lots(count: BigInt): Either[EconomicsError, Lots] =
    val coordinate = positionGrid.fromCoordinate(count)
    Positive(coordinate)
      .left
      .map(_ => InvalidLots(count))
      .map: positive =>
        InstrumentLots(
          identity.id,
          PositiveWhole(positionGrid.coordinate(positive.unrefined)).toOption.get,
          positionGrid.asQuantity(positive.unrefined)
        )

  def positionLots(side: Side, lots: Lots): Either[EconomicsError, PositionLots] =
    InstrumentIdentityChecks
      .check("positionLots", identity.id, "lots" -> lots.instrumentId)
      .map: _ =>
        val count = side.sign * lots.count.unrefined
        InstrumentPosition(identity.id, count, positionGrid.asQuantity(positionGrid.fromCoordinate(count)))

  val flatPosition: PositionLots =
    InstrumentPosition(identity.id, BigInt(0), positionGrid.asQuantity(positionGrid.fromCoordinate(0)))

  val prices: InstrumentPrices[roles.base.D, roles.quote.D] =
    new InstrumentPrices(identity.id, roles.base, roles.quote, priceGrid)
  val market: InstrumentMarket[roles.base.D, roles.quote.D, roles.settle.D] =
    new InstrumentMarket(identity.id, roles.base, roles.quote, roles.settle)
  val orders: InstrumentOrders[roles.position.D, roles.base.D, roles.quote.D] =
    new InstrumentOrders(identity.id)
  val scenarios: InstrumentScenarios[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new InstrumentScenarios(identity.id, positionGrid)
  val fees: InstrumentFees[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new InstrumentFees(identity.id, roles.settle)

  private val typedBasePerPosition =
    contractPayoff.basePerPosition.asInstanceOf[Rate[roles.position.D, roles.base.D]]
  private val typedQuotePerPosition =
    contractPayoff.quotePerPosition.asInstanceOf[Rate[roles.position.D, roles.quote.D]]

  val valuation: InstrumentValuation[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new InstrumentValuation(
      identity.id,
      roles.position,
      roles.settle,
      typedBasePerPosition,
      typedQuotePerPosition
    )
  val sizing: InstrumentSizing[roles.position.D, roles.base.D, roles.quote.D, roles.settle.D] =
    new InstrumentSizing(identity.id, roles.settle.dimension.asDimensionRef, lots, valuation)

end Instrument

object Instrument:
  /** Final validated definition boundary. */
  def create(definition: InstrumentDefinition): Either[EconomicsError, Instrument] =
    val identity   = definition.identity
    val roles      = definition.roles
    val listing    = definition.listingRules
    val payoff     = definition.contractPayoff
    val roleAssets = Vector("quote" -> roles.quote, "position" -> roles.position, "settle" -> roles.settle)

    roleAssets
      .collectFirst:
        case (role, candidate) if !roles.base.dimension.sharesRegistryWith(candidate.dimension) =>
          ForeignRegistry(role, roles.base.dimension.key, candidate.dimension.key)
      .map(Left(_))
      .getOrElse:
        if !listing.roles.eq(roles) then
          Left(ContradictoryInstrument(identity.id, InstrumentContradiction.ListingRolesDiffer))
        else if !payoff.roles.eq(roles) then
          Left(ContradictoryInstrument(identity.id, InstrumentContradiction.PayoffRolesDiffer))
        else if roles.base.id == roles.quote.id then
          Left(ContradictoryInstrument(identity.id, InstrumentContradiction.BaseEqualsQuote))
        else if !listing.positionLotGrid.dimension.sharesRegistryWith(roles.position.dimension) then
          Left(ForeignRegistry("position grid", roles.position.dimension.key, listing.positionLotGrid.dimension.key))
        else if listing.positionLotGrid.dimension.key != roles.position.dimension.key then
          Left(
            GridDimensionFailure(
              "position grid",
              listing.positionLotGrid.key,
              roles.position.dimension.key,
              listing.positionLotGrid.dimension.key
            )
          )
        else
          val expectedPrice =
            DimRef.divide(roles.quote.dimension.asDimensionRef, roles.base.dimension.asDimensionRef).key
          if !listing.priceGrid.dimension.sharesRegistryWith(roles.base.dimension) then
            Left(ForeignRegistry("price grid", expectedPrice, listing.priceGrid.dimension.key))
          else if listing.priceGrid.dimension.key != expectedPrice then
            Left(
              GridDimensionFailure("price grid", listing.priceGrid.key, expectedPrice, listing.priceGrid.dimension.key)
            )
          else if payoff.basePerPosition.coefficient.isZero && payoff.quotePerPosition.coefficient.isZero then
            Left(EmptyContractPayoff(identity.id))
          else
            val positionGrid = listing.positionLotGrid.asInstanceOf[RegisteredGridRef[roles.position.D]]
            val priceGrid    = listing.priceGrid.asInstanceOf[RegisteredGridRef[Divide[roles.quote.D, roles.base.D]]]
            Right(new Instrument(identity, roles, listing, payoff)(positionGrid, priceGrid))
  end create

end Instrument
