package external.economics.fixtures

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

object SharedEconomicsSetup:
  val baseDefinition = AssetDefinition(AssetId.from("shape-base").toOption.get, AtomId("shape:base"))
  val quoteDefinition = AssetDefinition(AssetId.from("shape-quote").toOption.get, AtomId("shape:quote"))
  val positionDefinition = AssetDefinition(AssetId.from("shape-position").toOption.get, AtomId("shape:position"))
  val baseDimensionKey = DimKey.atom(baseDefinition.dimensionAtom)
  val quoteDimensionKey = DimKey.atom(quoteDefinition.dimensionAtom)
  val positionDimensionKey = DimKey.atom(positionDefinition.dimensionAtom)
  val priceDimensionKey = DimKey.multiply(quoteDimensionKey, DimKey.inverse(baseDimensionKey))
  val lotsDefinition = GridDefinition(
    GridIdentity(
      positionDimensionKey,
      GridKey(GridId.from("shape-lots").toOption.get, GridVersion.from(1).toOption.get)
    ),
    PositiveRational(Rational.one).toOption.get
  )
  val priceDefinition = GridDefinition(
    GridIdentity(
      priceDimensionKey,
      GridKey(GridId.from("shape-prices").toOption.get, GridVersion.from(1).toOption.get)
    ),
    PositiveRational(Rational.one).toOption.get
  )
  val settleDefinition = GridDefinition(
    GridIdentity(
      quoteDimensionKey,
      GridKey(GridId.from("shape-settle").toOption.get, GridVersion.from(1).toOption.get)
    ),
    PositiveRational(Rational(1, 100)).toOption.get
  )
  val initialCatalogBatch = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterDimension(priceDimensionKey),
    CatalogCommand.RegisterGrid(lotsDefinition),
    CatalogCommand.RegisterGrid(settleDefinition)
  )
  val initialCatalogState = CatalogModel
    .commit(CatalogRoot.create().initialState, initialCatalogBatch)
    .toOption
    .get
    .state
  val olderSnapshot = initialCatalogState.snapshot
  val catalogSnapshot = CatalogModel
    .commit(initialCatalogState, CatalogBatch.one(CatalogCommand.RegisterGrid(priceDefinition)))
    .toOption
    .get
    .state
    .snapshot
  val base = catalogSnapshot.resolveAsset(baseDefinition.id).toOption.get
  val quote = catalogSnapshot.resolveAsset(quoteDefinition.id).toOption.get
  val position = catalogSnapshot.resolveAsset(positionDefinition.id).toOption.get
  val lotsGrid = catalogSnapshot.resolveGrid(position.dimension)(lotsDefinition.key).toOption.get
  val priceDimension = catalogSnapshot.resolveDimension(priceDimensionKey).toOption.get
  val priceGrid = catalogSnapshot.resolveGrid(priceDimension)(priceDefinition.key).toOption.get
  val settleGrid = catalogSnapshot.resolveGrid(quote.dimension)(settleDefinition.key).toOption.get
  val identity = InstrumentIdentity(
    InstrumentId.from("shape-instrument").toOption.get,
    UnderlyingId.from("shape-underlying").toOption.get
  )
  val definition = InstrumentDefinition(
    identity,
    AssetRoleIds(baseDefinition.id, quoteDefinition.id, positionDefinition.id, quoteDefinition.id),
    ListingDefinition(lotsDefinition.identity, priceDefinition.identity),
    PayoffDefinition(Rational.one, Rational.zero)
  )
  val spec       = InstrumentAssembler.assemble(definition, catalogSnapshot).toOption.get
  val instrument = Instrument.fromSpec(spec)
  val lots       = instrument.lots(2).toOption.get
  val price99    = instrument.prices.exact(Rational(99)).toOption.get
  val price100   = instrument.prices.exact(Rational(100)).toOption.get
  val state      = instrument.market.quoteSettled(price100).toOption.get
  val slice      = instrument.scenarios.slice(lots, state, LiquidityRole.Taker).toOption.get

  val marketOrder = instrument.orders.market(Side.Buy, lots).toOption.get
  val fixed = instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
  val fixedOrder    = instrument.orders.stopMarket(Side.Buy, lots, fixed).toOption.get
  val fixedEvidence = instrument.orders.fixedEvidence(fixed)(price100).toOption.get
  val trailing = instrument.orders
    .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, 1)
    .toOption
    .get
  val trailingOrder = instrument.orders.stopMarket(Side.Buy, lots, trailing).toOption.get
  val trailingEvidence = instrument.orders.trailingEvidence(trailing)(price99, price100).toOption.get
  val directLimit = instrument.orders.limit(Side.Buy, lots, price100).toOption.get
  val peg         = instrument.orders.peggedPricing(PriceReference.Mark, 1)
  val pegExecution = instrument.orders.pricedExecution(
    peg,
    TimeInForce.GoodTillCancelled,
    LiquidityConstraint.Unrestricted,
    instrument.orders.displayed
  )
  val peggedOrder = instrument.orders
    .create(instrument.orders.intent(Side.Buy, lots), instrument.orders.immediate, pegExecution)
    .toOption
    .get
  val pegResolution = instrument.orders.pegResolution(peg)(price99, price100).toOption.get

end SharedEconomicsSetup
