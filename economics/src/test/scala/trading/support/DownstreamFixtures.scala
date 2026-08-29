package trading.support

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

final class DownstreamFixtures:
  private val names  = Vector("btc", "usd", "contract", "token")
  private val assets = names.map: name =>
    AssetDefinition(AssetId.from(name).toOption.get, AtomId(s"asset:$name"))
  private val grids = Vector(
    grid("contract", "lots", Rational(1, 1000)),
    grid("usd", "cents", Rational(1, 100)),
    grid("token", "millis", Rational(1, 1000))
  )
  private val initial = CatalogModel
    .commit(
      CatalogRoot.create().initialState,
      CatalogBatch.from(
        assets.map(CatalogCommand.RegisterAsset.apply) ++ grids.map(CatalogCommand.RegisterGrid.apply)
      ).toOption.get
    )
    .toOption
    .get
    .state

  private val initialSnapshot = initial.snapshot
  val btc                     = asset("btc", initialSnapshot)
  val usd                     = asset("usd", initialSnapshot)
  val contract                = asset("contract", initialSnapshot)
  val token                   = asset("token", initialSnapshot)
  val contractLots            = resolvedGrid(contract, "lots", initialSnapshot)
  val usdCents                = resolvedGrid(usd, "cents", initialSnapshot)
  val tokenMillis             = resolvedGrid(token, "millis", initialSnapshot)

  private val priceDimension  = DimRef.divide(usd.dimension.ref, btc.dimension.ref).key
  private val priceDefinition = GridDefinition(
    GridIdentity(
      priceDimension,
      GridKey(GridId.from("half-dollar").toOption.get, GridVersion.from(1).toOption.get)
    ),
    PositiveRational(Rational(1, 2)).toOption.get
  )
  private val state = CatalogModel
    .commit(
      initial,
      CatalogBatch.of(
        CatalogCommand.RegisterDimension(priceDimension),
        CatalogCommand.RegisterGrid(priceDefinition)
      )
    )
    .toOption
    .get
    .state
  val snapshot  = state.snapshot
  val priceGrid = snapshot.resolveGrid(priceDefinition.identity).toOption.get

  val linear: Instrument  = instrument("linear", usd)
  val foreign: Instrument = instrument("foreign", usd)

  def price(instrument: Instrument, coefficient: Rational): instrument.Price =
    Price.exact(instrument)(coefficient).toOption.get

  def state(instrument: Instrument, coefficient: Rational): instrument.MarketState =
    MarketState.quoteSettled(instrument)(price(instrument, coefficient)).toOption.get

  private def instrument(id: String, settlement: Asset): Instrument =
    val definition = InstrumentDefinition(
      InstrumentIdentity(
        InstrumentId.from(id).toOption.get,
        UnderlyingId.from("bitcoin").toOption.get
      ),
      AssetRoleIds(btc.id, usd.id, contract.id, settlement.id),
      ListingDefinition(contractLots.identity, priceGrid.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )
    Instrument.fromSpec(InstrumentAssembler.assemble(definition, snapshot).toOption.get)

  private def grid(assetName: String, name: String, quantum: Rational): GridDefinition =
    GridDefinition(
      GridIdentity(
        DimKey.atom(AtomId(s"asset:$assetName")),
        GridKey(GridId.from(name).toOption.get, GridVersion.from(1).toOption.get)
      ),
      PositiveRational(quantum).toOption.get
    )

  private def asset(name: String, snapshot: CatalogSnapshot): Asset =
    snapshot.resolveAsset(AssetId.from(name).toOption.get).toOption.get

  private def resolvedGrid(asset: Asset, name: String, snapshot: CatalogSnapshot): GridHandle[asset.D] =
    snapshot
      .resolveGrid(asset.dimension)(GridKey(GridId.from(name).toOption.get, GridVersion.from(1).toOption.get))
      .toOption
      .get
end DownstreamFixtures
