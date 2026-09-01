package external.execution.fixtures

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

object ExecutionLifecycleSetup:
  val baseDefinition     = AssetDefinition(AssetId.from("execution-base").toOption.get, AtomId("execution:base"))
  val quoteDefinition    = AssetDefinition(AssetId.from("execution-quote").toOption.get, AtomId("execution:quote"))
  val positionDefinition =
    AssetDefinition(AssetId.from("execution-position").toOption.get, AtomId("execution:position"))
  val baseKey     = DimKey.atom(baseDefinition.dimensionAtom)
  val quoteKey    = DimKey.atom(quoteDefinition.dimensionAtom)
  val positionKey = DimKey.atom(positionDefinition.dimensionAtom)
  val priceKey    = DimKey.multiply(quoteKey, DimKey.inverse(baseKey))
  val lotsDefinition = GridDefinition(
    GridIdentity(
      positionKey,
      GridKey(GridId.from("execution-lots").toOption.get, GridVersion.from(1).toOption.get)
    ),
    PositiveRational(Rational.one).toOption.get
  )
  val priceDefinition = GridDefinition(
    GridIdentity(
      priceKey,
      GridKey(GridId.from("execution-price").toOption.get, GridVersion.from(1).toOption.get)
    ),
    PositiveRational(Rational.one).toOption.get
  )
  val batch = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterDimension(priceKey),
    CatalogCommand.RegisterGrid(lotsDefinition),
    CatalogCommand.RegisterGrid(priceDefinition)
  )
  val snapshot   = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get.state.snapshot
  val base       = snapshot.resolveAsset(baseDefinition.id).toOption.get
  val quote      = snapshot.resolveAsset(quoteDefinition.id).toOption.get
  val position   = snapshot.resolveAsset(positionDefinition.id).toOption.get
  val lotsGrid   = snapshot.resolveGrid(position.dimension)(lotsDefinition.key).toOption.get
  val priceBasis = snapshot.resolveDimension(priceKey).toOption.get
  val priceGrid  = snapshot.resolveGrid(priceBasis)(priceDefinition.key).toOption.get
  val definition = InstrumentDefinition(
    InstrumentIdentity(
      InstrumentId.from("execution-instrument").toOption.get,
      UnderlyingId.from("execution-underlying").toOption.get
    ),
    AssetRoleIds(base.id, quote.id, position.id, quote.id),
    ListingDefinition(lotsGrid.identity, priceGrid.identity),
    PayoffDefinition(Rational.one, Rational.zero)
  )
  val instrument = Instrument.fromSpec(InstrumentAssembler.assemble(definition, snapshot).toOption.get)
  val lots       = Lots.fromCount(instrument)(2).toOption.get
  val order      = Order.market(instrument)(Side.Buy, lots).toOption.get
end ExecutionLifecycleSetup
