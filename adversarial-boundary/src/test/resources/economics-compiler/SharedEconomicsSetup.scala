package external.economics.fixtures

import trading.economics.instrument.*
import trading.fee.policy.*
import trading.order.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*
import trading.scenario.*

object SharedEconomicsSetup:
  val baseDefinition     = AssetDefinition(AssetId.from("shape-base").toOption.get, AtomId("shape:base"))
  val quoteDefinition    = AssetDefinition(AssetId.from("shape-quote").toOption.get, AtomId("shape:quote"))
  val positionDefinition = AssetDefinition(AssetId.from("shape-position").toOption.get, AtomId("shape:position"))
  val baseKey             = DimKey.atom(baseDefinition.dimensionAtom)
  val quoteKey            = DimKey.atom(quoteDefinition.dimensionAtom)
  val positionKey         = DimKey.atom(positionDefinition.dimensionAtom)
  val priceKey            = DimKey.multiply(quoteKey, DimKey.inverse(baseKey))
  val lotsDefinition = GridDefinition(
    GridIdentity(positionKey, GridKey(GridId.from("shape-lots").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational.one).toOption.get
  )
  val priceDefinition = GridDefinition(
    GridIdentity(priceKey, GridKey(GridId.from("shape-price").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational.one).toOption.get
  )
  val quoteGridDefinition = GridDefinition(
    GridIdentity(quoteKey, GridKey(GridId.from("shape-quote-grid").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational(1, 100)).toOption.get
  )
  val batch = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterDimension(priceKey),
    CatalogCommand.RegisterGrid(lotsDefinition),
    CatalogCommand.RegisterGrid(priceDefinition),
    CatalogCommand.RegisterGrid(quoteGridDefinition)
  )
  val snapshot = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get.state.snapshot
  val base      = snapshot.resolveAsset(baseDefinition.id).toOption.get
  val quote     = snapshot.resolveAsset(quoteDefinition.id).toOption.get
  val position  = snapshot.resolveAsset(positionDefinition.id).toOption.get
  val lotsGrid  = snapshot.resolveGrid(position.dimension)(lotsDefinition.key).toOption.get
  val quoteGrid = snapshot.resolveGrid(quote.dimension)(quoteGridDefinition.key).toOption.get
  val priceDimension = snapshot.resolveDimension(priceKey).toOption.get
  val priceGrid       = snapshot.resolveGrid(priceDimension)(priceDefinition.key).toOption.get
  val identity = InstrumentIdentity(
    InstrumentId.from("shape-instrument").toOption.get,
    UnderlyingId.from("shape-underlying").toOption.get
  )
  val definition = InstrumentDefinition(
    identity,
    AssetRoleIds(base.id, quote.id, position.id, quote.id),
    ListingDefinition(lotsGrid.identity, priceGrid.identity),
    PayoffDefinition(Rational.one, Rational.zero)
  )
  val spec        = InstrumentAssembler.assemble(definition, snapshot).toOption.get
  val instrument  = Instrument.fromSpec(spec)
  type D           = instrument.roles.position.D
  type B           = instrument.roles.base.D
  type Q           = instrument.roles.quote.D
  val lots        = Lots.fromCount(instrument)(2).toOption.get
  val price99     = Price.exact(instrument)(Rational(99)).toOption.get
  val price100    = Price.exact(instrument)(Rational(100)).toOption.get
  val state       = MarketState.quoteSettled(instrument)(price100).toOption.get
  val feePolicy   = FeePolicy(instrument)
  val marketOrder = Order.market(instrument)(Side.Buy, lots).toOption.get
  val slice       = LiquiditySlice.create(instrument)(lots, state, LiquidityRole.Taker).toOption.get
end SharedEconomicsSetup
