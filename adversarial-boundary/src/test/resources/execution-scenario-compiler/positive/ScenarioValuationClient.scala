package external.scenario.positive

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*
import trading.scenario.*

object ScenarioValuationClient:
  private val baseDefinition     = AssetDefinition(AssetId.from("scenario-base").toOption.get, AtomId("scenario:base"))
  private val quoteDefinition    = AssetDefinition(AssetId.from("scenario-quote").toOption.get, AtomId("scenario:quote"))
  private val positionDefinition =
    AssetDefinition(AssetId.from("scenario-position").toOption.get, AtomId("scenario:position"))
  private val baseKey     = DimKey.atom(baseDefinition.dimensionAtom)
  private val quoteKey    = DimKey.atom(quoteDefinition.dimensionAtom)
  private val positionKey = DimKey.atom(positionDefinition.dimensionAtom)
  private val priceKey    = DimKey.multiply(quoteKey, DimKey.inverse(baseKey))
  private val lotsDefinition = GridDefinition(
    GridIdentity(positionKey, GridKey(GridId.from("scenario-lots").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational.one).toOption.get
  )
  private val priceDefinition = GridDefinition(
    GridIdentity(priceKey, GridKey(GridId.from("scenario-price").toOption.get, GridVersion.from(1).toOption.get)),
    PositiveRational(Rational.one).toOption.get
  )
  private val batch = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterDimension(priceKey),
    CatalogCommand.RegisterGrid(lotsDefinition),
    CatalogCommand.RegisterGrid(priceDefinition)
  )
  private val snapshot = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get.state.snapshot
  private val base      = snapshot.resolveAsset(baseDefinition.id).toOption.get
  private val quote     = snapshot.resolveAsset(quoteDefinition.id).toOption.get
  private val position  = snapshot.resolveAsset(positionDefinition.id).toOption.get
  private val lotsGrid  = snapshot.resolveGrid(position.dimension)(lotsDefinition.key).toOption.get
  private val priceDimension = snapshot.resolveDimension(priceKey).toOption.get
  private val priceGrid       = snapshot.resolveGrid(priceDimension)(priceDefinition.key).toOption.get
  private val definition = InstrumentDefinition(
    InstrumentIdentity(
      InstrumentId.from("scenario-instrument").toOption.get,
      UnderlyingId.from("scenario-underlying").toOption.get
    ),
    AssetRoleIds(base.id, quote.id, position.id, quote.id),
    ListingDefinition(lotsGrid.identity, priceGrid.identity),
    PayoffDefinition(Rational.one, Rational.zero)
  )
  private val instrument = Instrument.fromSpec(InstrumentAssembler.assemble(definition, snapshot).toOption.get)
  private val lots       = Lots.fromCount(instrument)(2).toOption.get

  private def scenario(side: Side, price: Rational) =
    val order  = Order.market(instrument)(side, lots).toOption.get
    val market = MarketState.quoteSettled(instrument)(Price.exact(instrument)(price).toOption.get).toOption.get
    val slice  = LiquiditySlice.create(instrument)(lots, market, LiquidityRole.Taker).toOption.get
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice
    )
    OrderScenario.evaluate(instrument)(assumptions).toOption.get

  private val trip = RoundTripScenario
    .create(instrument)(scenario(Side.Buy, Rational(99)), scenario(Side.Sell, Rational(100)))
    .toOption
    .get
  private val result = ScenarioValuation.pricePnl(instrument)(trip).toOption.get

  assert(result.instrumentId == instrument.identity.id)
  assert(result.quantity.coefficient == Rational(2))
end ScenarioValuationClient
