package external.economics.positive

import trading.economics.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.*
import trading.quantity.runtime.*

object CompleteEconomicsClient:
  def genericLotCount(instrument: Instrument)(lots: instrument.Lots): BigInt =
    instrument.lotCount(lots)

  def genericPnl(
    instrument: Instrument
  )(
    roundTrip: instrument.RoundTripScenario,
    schedule: instrument.FeeSchedule
  ): Either[EconomicsError, instrument.Pnl] =
    instrument.calculatePnl(roundTrip, schedule)

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
  val instrument = Instrument
    .create(
      InstrumentId("client-instrument"),
      UnderlyingId("client-underlying"),
      base,
      quote,
      position,
      quote
    )(
      lotsGrid,
      priceGrid,
      Rate(position.dimension.asDimensionRef, base.dimension.asDimensionRef, Rational.one),
      Rate(position.dimension.asDimensionRef, quote.dimension.asDimensionRef, Rational.zero)
    )
    .toOption
    .get

  val lots       = instrument.lots(2).toOption.get
  val entryPrice = instrument.price(200).toOption.get
  val exitPrice  = instrument.price(220).toOption.get
  val entryState = instrument.marketStateForQuote(entryPrice).toOption.get
  val exitState  = instrument.marketStateForQuote(exitPrice).toOption.get
  val entryOrder = instrument.marketOrder(Side.Buy, lots).toOption.get
  val exitOrder  = instrument.marketOrder(Side.Sell, lots).toOption.get
  val entrySlice = instrument.liquiditySlice(lots, entryState, LiquidityRole.Taker)
  val exitSlice  = instrument.liquiditySlice(lots, exitState, LiquidityRole.Maker)
  val entry      = instrument.orderScenario(entryOrder, Vector(entrySlice)).toOption.get
  val exit       = instrument.orderScenario(exitOrder, Vector(exitSlice)).toOption.get
  val roundTrip  = instrument.roundTrip(entry, exit).toOption.get

  val schedule = new instrument.FeeSchedule:
    def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
      val basis = Quantity(quote.dimension.asDimensionRef, Rational(instrument.lotCount(scenario.order.lots)))
      for
        fee <- instrument.percentageFee(quote)(
          quoteGrid,
          FeeKind("client-fee"),
          basis,
          FeeRate(Rational(1, 1000)),
          QuantizationPolicy.TowardZero
        )
        line <- instrument.feeLine(scenario, 0, fee)
      yield Vector(line)

  val pnl = instrument.calculatePnl(roundTrip, schedule).toOption.get
  val genericResult = genericPnl(instrument)(roundTrip, schedule)
  val genericCount  = genericLotCount(instrument)(lots)
  val sized = instrument.sizePosition(
    Quantity(instrument.settle.dimension.asDimensionRef, Rational(1000)),
    PositiveWhole(3).toOption.get,
    schedule
  ): candidate =>
    val candidateEntryOrder = instrument.marketOrder(Side.Buy, candidate).toOption.get
    val candidateExitOrder  = instrument.marketOrder(Side.Sell, candidate).toOption.get
    val candidateEntry = instrument.orderScenario(
      candidateEntryOrder,
      Vector(instrument.liquiditySlice(candidate, entryState, LiquidityRole.Taker))
    ).toOption.get
    val candidateExit = instrument.orderScenario(
      candidateExitOrder,
      Vector(instrument.liquiditySlice(candidate, exitState, LiquidityRole.Taker))
    ).toOption.get
    instrument.roundTrip(candidateEntry, candidateExit)

end CompleteEconomicsClient
