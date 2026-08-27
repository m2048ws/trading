package external.economics.positive

import trading.economics.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.*
import trading.quantity.runtime.*

object CompleteEconomicsClient:
  def genericLotCount(instrument: Instrument)(lots: instrument.Lots): BigInt = lots.count.unrefined

  def genericPrice(
    instrument: Instrument
  )(coefficient: Rational): Either[EconomicsError, instrument.Price] =
    instrument.prices.exact(coefficient)

  def genericOrderLots(instrument: Instrument)(order: instrument.Order): BigInt =
    order.intent.lots.count.unrefined

  def genericPnl(
    instrument: Instrument
  )(
    roundTrip: instrument.RoundTripScenario,
    schedule: instrument.FeeSchedule
  ): Either[EconomicsError, instrument.Pnl] =
    instrument.valuation.pnl(roundTrip, schedule)

  val registry = new QuantityRegistry
  val base = registry.registerAsset(AssetDefinition(AssetId("client-base"), AtomId("client:base"))).toOption.get
  val quote = registry.registerAsset(AssetDefinition(AssetId("client-quote"), AtomId("client:quote"))).toOption.get
  val position =
    registry.registerAsset(AssetDefinition(AssetId("client-position"), AtomId("client:position"))).toOption.get
  val lotsGrid = registry
    .registerGrid(position)(
      GridDefinition(
        position.dimension.key,
        GridId("client-lots"),
        GridVersion(1),
        PositiveRational(Rational.one).toOption.get
      )
    )
    .toOption
    .get
  val quoteGrid = registry
    .registerGrid(quote)(
      GridDefinition(
        quote.dimension.key,
        GridId("client-quote-grid"),
        GridVersion(1),
        PositiveRational(Rational(1, 100)).toOption.get
      )
    )
    .toOption
    .get
  val priceDimension =
    registry.registerDimension(DimRef.divide(quote.dimension.asDimensionRef, base.dimension.asDimensionRef).key).toOption.get
  val priceGrid = registry
    .registerGrid(priceDimension)(
      GridDefinition(
        priceDimension.dimension.key,
        GridId("client-price-grid"),
        GridVersion(1),
        PositiveRational(Rational(1, 2)).toOption.get
      )
    )
    .toOption
    .get

  val roles = new InstrumentRoles(base, quote, position, quote)
  val identity = InstrumentIdentity(InstrumentId("client-instrument"), UnderlyingId("client-underlying"))
  val listing = new ListingRules(roles)(lotsGrid, priceGrid)
  val payoff = new ContractPayoff(roles)(
    Rate(roles.position.dimension.asDimensionRef, roles.base.dimension.asDimensionRef, Rational.one),
    Rate(roles.position.dimension.asDimensionRef, roles.quote.dimension.asDimensionRef, Rational.zero)
  )
  val instrument = Instrument.create(InstrumentDefinition(identity, roles, listing, payoff)).toOption.get
  val stable = instrument

  val lots = stable.lots(2).toOption.get
  val entryPrice = stable.prices.exact(Rational(100)).toOption.get
  val exitPrice = stable.prices.exact(Rational(110)).toOption.get
  val entryState = stable.market.quoteSettled(entryPrice).toOption.get
  val exitState = stable.market.quoteSettled(exitPrice).toOption.get
  val entryOrder = stable.orders.market(Side.Buy, lots).toOption.get
  val exitOrder = stable.orders.market(Side.Sell, lots).toOption.get
  val entrySlice = stable.scenarios.slice(lots, entryState, LiquidityRole.Taker)
  val exitSlice = stable.scenarios.slice(lots, exitState, LiquidityRole.Taker)
  val entryAssumptions = stable.scenarios.assumptions(
    stable.scenarios.immediate,
    stable.scenarios.directPricing,
    Vector(entrySlice)
  )
  val exitAssumptions = stable.scenarios.assumptions(
    stable.scenarios.immediate,
    stable.scenarios.directPricing,
    Vector(exitSlice)
  )
  val entry = stable.scenarios.order(entryOrder, entryAssumptions).toOption.get
  val exit = stable.scenarios.order(exitOrder, exitAssumptions).toOption.get
  val roundTrip = stable.scenarios.roundTrip(entry, exit).toOption.get
  val denomination = stable.fees
    .denomination(quote)(quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get

  val schedule = new stable.FeeSchedule:
    def assess(scenario: stable.OrderScenario): Either[EconomicsError, Vector[stable.FeeLine]] =
      val basis = Quantity(quote.dimension.asDimensionRef, Rational(scenario.order.intent.lots.count.unrefined))
      for
        fee <- denomination.percentage(FeeKind("client-fee"), basis, FeeRate(Rational(1, 1000)))
        line <- stable.fees.line(scenario, 0, fee)
      yield Vector(line)

  val pnl = stable.valuation.pnl(roundTrip, schedule).toOption.get
  val genericResult = genericPnl(stable)(roundTrip, schedule)
  val genericCount = genericLotCount(stable)(lots)
  val genericOrderCount = genericOrderLots(stable)(entryOrder)
  val genericEntryPrice = genericPrice(stable)(Rational(100))
  val sized = stable.sizing.maxLots(
    Quantity(stable.roles.settle.dimension.asDimensionRef, Rational(1000)),
    PositiveWhole(3).toOption.get,
    schedule
  ): candidate =>
    val candidateEntryOrder = stable.orders.market(Side.Buy, candidate).toOption.get
    val candidateExitOrder = stable.orders.market(Side.Sell, candidate).toOption.get
    val candidateEntrySlice = stable.scenarios.slice(candidate, entryState, LiquidityRole.Taker)
    val candidateExitSlice = stable.scenarios.slice(candidate, exitState, LiquidityRole.Taker)
    val candidateEntry = stable.scenarios.order(
      candidateEntryOrder,
      stable.scenarios.assumptions(stable.scenarios.immediate, stable.scenarios.directPricing, Vector(candidateEntrySlice))
    ).toOption.get
    val candidateExit = stable.scenarios.order(
      candidateExitOrder,
      stable.scenarios.assumptions(stable.scenarios.immediate, stable.scenarios.directPricing, Vector(candidateExitSlice))
    ).toOption.get
    stable.scenarios.roundTrip(candidateEntry, candidateExit)

end CompleteEconomicsClient
