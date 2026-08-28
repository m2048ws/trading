package external.economics.positive

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.*
import trading.reference.*

object CompleteEconomicsClient:
  def genericLotCount(instrument: Instrument)(lots: instrument.Lots): BigInt = lots.count.unrefined

  def genericPrice(
    instrument: Instrument
  )(coefficient: Rational): Either[EconomicsError, instrument.Price] =
    instrument.prices.exact(coefficient)

  def genericOrderLots(instrument: Instrument)(order: instrument.Order): BigInt =
    order.intent.lots.count.unrefined

  def genericActivation[P](activation: OrderActivation[P])(
    evidence: activation.Evidence
  ): Either[ActivationViolation, CheckedActivation[P]] =
    activation.validate(evidence)

  def genericPricing[L, P](execution: OrderExecution[L, P])(
    resolution: execution.Resolution
  ): Either[PricingViolation, EffectivePricing[P]] =
    execution.resolve(resolution)

  def genericPnl(
    instrument: Instrument
  )(
    roundTrip: instrument.RoundTripScenario,
    schedule: instrument.FeeSchedule
  ): Either[EconomicsError, instrument.Pnl] =
    instrument.valuation.pnl(roundTrip, schedule)

  val baseDefinition = AssetDefinition(AssetId.from("client-base").toOption.get, AtomId("client:base"))
  val quoteDefinition = AssetDefinition(AssetId.from("client-quote").toOption.get, AtomId("client:quote"))
  val positionDefinition = AssetDefinition(AssetId.from("client-position").toOption.get, AtomId("client:position"))
  val baseKey = DimKey.atom(baseDefinition.dimensionAtom)
  val quoteKey = DimKey.atom(quoteDefinition.dimensionAtom)
  val positionKey = DimKey.atom(positionDefinition.dimensionAtom)
  val priceKey = DimKey.multiply(quoteKey, DimKey.inverse(baseKey))
  val lotsDefinition = GridDefinition(
    GridIdentity(positionKey, GridKey(GridId.from("client-lots").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational.one).toOption.get
  )
  val quoteGridDefinition = GridDefinition(
    GridIdentity(quoteKey, GridKey(GridId.from("client-quote-grid").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational(1, 100)).toOption.get
  )
  val priceGridDefinition = GridDefinition(
    GridIdentity(priceKey, GridKey(GridId.from("client-price-grid").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational(1, 2)).toOption.get
  )
  val catalogBatch = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterDimension(priceKey),
    CatalogCommand.RegisterGrid(lotsDefinition),
    CatalogCommand.RegisterGrid(quoteGridDefinition),
    CatalogCommand.RegisterGrid(priceGridDefinition)
  )
  val catalogSnapshot = CatalogModel.commit(CatalogRoot.create().initialState, catalogBatch).toOption.get.state.snapshot
  val base = catalogSnapshot.resolveAsset(baseDefinition.id).toOption.get
  val quote = catalogSnapshot.resolveAsset(quoteDefinition.id).toOption.get
  val position = catalogSnapshot.resolveAsset(positionDefinition.id).toOption.get
  val lotsGrid = catalogSnapshot.resolveGrid(position.dimension)(lotsDefinition.key).toOption.get
  val quoteGrid = catalogSnapshot.resolveGrid(quote.dimension)(quoteGridDefinition.key).toOption.get
  val priceDimension = catalogSnapshot.resolveDimension(priceKey).toOption.get
  val priceGrid = catalogSnapshot.resolveGrid(priceDimension)(priceGridDefinition.key).toOption.get

  val roles = new Roles(base, quote, position, quote)
  val identity = Identity(InstrumentId("client-instrument"), UnderlyingId("client-underlying"))
  val listing = new ListingRules(roles)(lotsGrid, priceGrid)
  val payoff = new ContractPayoff(roles)(
    Rate(roles.position.dimension.ref, roles.base.dimension.ref, Rational.one),
    Rate(roles.position.dimension.ref, roles.quote.dimension.ref, Rational.zero)
  )
  val definition = Definition(identity, roles, listing, payoff)
  val validated = Instrument.validate(definition).toOption.get
  val instrument = Instrument.fromValidated(validated)
  val stable = instrument

  val lots = stable.lots(2).toOption.get
  val entryPrice = stable.prices.exact(Rational(100)).toOption.get
  val exitPrice = stable.prices.exact(Rational(110)).toOption.get
  val entryState = stable.market.quoteSettled(entryPrice).toOption.get
  val exitState = stable.market.quoteSettled(exitPrice).toOption.get
  val entryOrder = stable.orders.market(Side.Buy, lots).toOption.get
  val exitOrder = stable.orders.market(Side.Sell, lots).toOption.get
  val entrySlice = stable.scenarios.slice(lots, entryState, LiquidityRole.Taker).toOption.get
  val exitSlice = stable.scenarios.slice(lots, exitState, LiquidityRole.Taker).toOption.get
  val entryAssumptions = stable.scenarios.assumptionsOne(entryOrder)(
    entryOrder.activation.evidence,
    entryOrder.execution.resolution,
    entrySlice
  )
  val exitAssumptions = stable.scenarios.assumptionsOne(exitOrder)(
    exitOrder.activation.evidence,
    exitOrder.execution.resolution,
    exitSlice
  )
  val entry = stable.scenarios.order(entryOrder, entryAssumptions).toOption.get
  val exit = stable.scenarios.order(exitOrder, exitAssumptions).toOption.get
  val roundTrip = stable.scenarios.roundTrip(entry, exit).toOption.get
  val denomination = stable.fees
    .denomination(quote)(quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get

  val schedule = new stable.FeeSchedule:
    val instrumentId: InstrumentId = stable.identity.id
    def assess(scenario: stable.OrderScenario): Either[EconomicsError, Vector[stable.FeeLine]] =
      val basis = Quantity(quote.dimension.ref, Rational(scenario.order.intent.lots.count.unrefined))
      for
        fee <- denomination.percentage(FeeKind("client-fee"), basis, FeeRate(Rational(1, 1000)))
        line <- stable.fees.line(scenario, 0, fee)
      yield Vector(line)

  val pnl = stable.valuation.pnl(roundTrip, schedule).toOption.get
  val genericResult = genericPnl(stable)(roundTrip, schedule)
  val genericCount = genericLotCount(stable)(lots)
  val genericOrderCount = genericOrderLots(stable)(entryOrder)
  val genericEntryPrice = genericPrice(stable)(Rational(100))
  val directActivation = stable.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, entryPrice)
  val directEvidence = stable.orders.fixedEvidence(directActivation)(entryPrice).toOption.get
  val genericActivationResult = genericActivation(directActivation)(directEvidence)
  val genericPricingResult = genericPricing(entryOrder.execution)(entryOrder.execution.resolution)
  val matchedTriggerTicks = directActivation match
    case FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, triggerPrice) =>
      triggerPrice.ticks.unrefined
    case _ => BigInt(-1)
  val foreignIntent = OrderIntent(InstrumentId("foreign-instrument"), Side.Buy, lots, PositionEffect.Unrestricted)
  val runtimeMismatch = stable.orders.create(
    foreignIntent,
    stable.orders.immediate,
    stable.orders.marketExecution(NonRestingTimeInForce.ImmediateOrCancel)
  )
  assert(runtimeMismatch == Left(Mismatch("order.intent", stable.identity.id, foreignIntent.instrumentId)))
  val typedBaseConversion = stable.market.conversionFromRate(base)(
    Rate(base.dimension.ref, stable.roles.settle.dimension.ref, Rational(100))
  ).toOption.get
  assert(typedBaseConversion.rate.coefficient == Rational(100))
  val checkedBaseValue = entryState
    .convertToSettle(base)(Quantity(base.dimension.ref, Rational(1, 3)))
    .toOption
    .get
  assert(checkedBaseValue.coefficient == Rational(100, 3))
  val foreignBaseDefinition = AssetDefinition(base.id, AtomId("client:base"))
  val foreignBase = CatalogModel
    .commit(
      CatalogRoot.create().initialState,
      CatalogBatch.one(CatalogCommand.RegisterAsset(foreignBaseDefinition))
    )
    .toOption
    .get
    .state
    .snapshot
    .resolveAsset(base.id)
    .toOption
    .get
  val foreignLookup = entryState.convertToSettle(foreignBase)(
    Quantity(foreignBase.dimension.ref, Rational.one)
  )
  assert(foreignLookup.swap.exists(_.isInstanceOf[ForeignReferenceDataLineage]))
  val stop = stable.orders.stopMarket(Side.Buy, lots, directActivation).toOption.get
  val stopAssumptions = stable.scenarios.assumptionsOne(stop)(
    directEvidence,
    stop.execution.resolution,
    entrySlice
  )
  assert(stable.scenarios.order(stop, stopAssumptions).isRight)
  val sized = stable.sizing.maxLots(
    Quantity(stable.roles.settle.dimension.ref, Rational(1000)),
    PositiveWhole(3).toOption.get,
    schedule
  ): candidate =>
    val candidateEntryOrder = stable.orders.market(Side.Buy, candidate).toOption.get
    val candidateExitOrder = stable.orders.market(Side.Sell, candidate).toOption.get
    val candidateEntrySlice = stable.scenarios.slice(candidate, entryState, LiquidityRole.Taker).toOption.get
    val candidateExitSlice = stable.scenarios.slice(candidate, exitState, LiquidityRole.Taker).toOption.get
    val candidateEntry = stable.scenarios.order(
      candidateEntryOrder,
      stable.scenarios.assumptionsOne(candidateEntryOrder)(
        candidateEntryOrder.activation.evidence,
        candidateEntryOrder.execution.resolution,
        candidateEntrySlice
      )
    ).toOption.get
    val candidateExit = stable.scenarios.order(
      candidateExitOrder,
      stable.scenarios.assumptionsOne(candidateExitOrder)(
        candidateExitOrder.activation.evidence,
        candidateExitOrder.execution.resolution,
        candidateExitSlice
      )
    ).toOption.get
    stable.scenarios.roundTrip(candidateEntry, candidateExit)

end CompleteEconomicsClient
