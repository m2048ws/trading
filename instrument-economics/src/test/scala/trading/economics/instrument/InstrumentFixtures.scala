package trading.economics.instrument

import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

final class InstrumentFixtures:
  private val assetDefinitions = Vector("btc", "usd", "contract", "eur", "fee-token").map: name =>
    AssetDefinition(AssetId.from(name).toOption.get, AtomId(s"asset:$name"))

  private val baseGridDefinitions = Vector(
    gridDefinition("contract", "contract-lots", Rational(1, 1000)),
    gridDefinition("usd", "usd-cents", Rational(1, 100)),
    gridDefinition("btc", "btc-satoshis", Rational(1, 100_000_000)),
    gridDefinition("eur", "eur-cents", Rational(1, 100)),
    gridDefinition("fee-token", "token-millis", Rational(1, 1000))
  )

  private val baseBatch = CatalogBatch.from(
    assetDefinitions.map(CatalogCommand.RegisterAsset.apply) ++
      baseGridDefinitions.map(CatalogCommand.RegisterGrid.apply)
  ).toOption.get
  private val baseState       = CatalogModel.commit(CatalogRoot.create().initialState, baseBatch).toOption.get.state
  val snapshotBeforePriceGrid = baseState.snapshot

  val btc      = resolvedAsset("btc", snapshotBeforePriceGrid)
  val usd      = resolvedAsset("usd", snapshotBeforePriceGrid)
  val contract = resolvedAsset("contract", snapshotBeforePriceGrid)
  val eur      = resolvedAsset("eur", snapshotBeforePriceGrid)
  val token    = resolvedAsset("fee-token", snapshotBeforePriceGrid)

  val contractLots = resolvedGrid(contract, "contract-lots", snapshotBeforePriceGrid)
  val usdCents     = resolvedGrid(usd, "usd-cents", snapshotBeforePriceGrid)
  val btcSatoshis  = resolvedGrid(btc, "btc-satoshis", snapshotBeforePriceGrid)
  val eurCents     = resolvedGrid(eur, "eur-cents", snapshotBeforePriceGrid)
  val tokenMillis  = resolvedGrid(token, "token-millis", snapshotBeforePriceGrid)

  private val usdPerBtcDimension  = DimRef.divide(usd.dimension.ref, btc.dimension.ref).key
  private val usdPerBtcDefinition = GridDefinition(
    GridIdentity(
      usdPerBtcDimension,
      GridKey(
        GridId.from("usd-per-btc-half-dollar").toOption.get,
        GridVersion.from(1).toOption.get
      )
    ),
    PositiveRational(Rational(1, 2)).toOption.get
  )
  private val completeState = CatalogModel
    .commit(
      baseState,
      CatalogBatch.of(
        CatalogCommand.RegisterDimension(usdPerBtcDimension),
        CatalogCommand.RegisterGrid(usdPerBtcDefinition)
      )
    )
    .toOption
    .get
    .state
  val snapshot       = completeState.snapshot
  val usdPerBtcTicks = snapshot.resolveGrid(usdPerBtcDefinition.identity).toOption.get

  val linear: Instrument =
    instrument("linear-btcusd", "bitcoin-index", btc, usd, contract, usd)(
      contractLots,
      usdPerBtcTicks,
      Rational.one,
      Rational.zero
    )

  val inverse: Instrument =
    instrument("inverse-btcusd", "bitcoin-index", btc, usd, contract, btc)(
      contractLots,
      usdPerBtcTicks,
      Rational.zero,
      Rational(-100)
    )

  val quanto: Instrument =
    instrument("quanto-btcusd-eur", "bitcoin-index", btc, usd, contract, eur)(
      contractLots,
      usdPerBtcTicks,
      Rational.one,
      Rational.zero
    )

  val foreignIdentity: Instrument =
    instrument("foreign-linear-btcusd", "bitcoin-index", btc, usd, contract, usd)(
      contractLots,
      usdPerBtcTicks,
      Rational.one,
      Rational.zero
    )

  private val foreignAssetDefinition =
    AssetDefinition(AssetId.from("foreign-usd").toOption.get, AtomId("asset:usd"))
  private val foreignGridDefinition = gridDefinition("usd", "foreign-usd-cents", Rational(1, 100))
  private val foreignSnapshot       = CatalogModel
    .commit(
      CatalogRoot.create().initialState,
      CatalogBatch.of(
        CatalogCommand.RegisterAsset(foreignAssetDefinition),
        CatalogCommand.RegisterGrid(foreignGridDefinition)
      )
    )
    .toOption
    .get
    .state
    .snapshot
  val foreignUsd      = foreignSnapshot.resolveAsset(foreignAssetDefinition.id).toOption.get
  val foreignUsdCents = foreignSnapshot
    .resolveGrid(foreignUsd.dimension)(foreignGridDefinition.key)
    .toOption
    .get

  def lots(instrument: Instrument, count: BigInt): instrument.Lots =
    Lots.fromCount(instrument)(count).toOption.get

  def position(instrument: Instrument, coordinate: BigInt): instrument.PositionLots =
    PositionLots.fromCoordinate(instrument)(coordinate)

  def price(instrument: Instrument, coefficient: Rational): instrument.Price =
    Price.exact(instrument)(coefficient).toOption.get

  def quoteState(instrument: Instrument, coefficient: Rational): instrument.MarketState =
    MarketState.quoteSettled(instrument)(price(instrument, coefficient)).toOption.get

  def instrument(
    id: String,
    underlying: String,
    base: Asset,
    quote: Asset,
    position: Asset,
    settle: Asset
  )(
    positionGrid: GridHandle[? <: Dim],
    priceGrid: GridHandle[? <: Dim],
    baseCoefficient: Rational,
    quoteCoefficient: Rational
  ): Instrument =
    val definition = InstrumentDefinition(
      InstrumentIdentity(
        InstrumentId.from(id).toOption.get,
        UnderlyingId.from(underlying).toOption.get
      ),
      AssetRoleIds(base.id, quote.id, position.id, settle.id),
      ListingDefinition(positionGrid.identity, priceGrid.identity),
      PayoffDefinition(baseCoefficient, quoteCoefficient)
    )
    Instrument.fromSpec(InstrumentAssembler.assemble(definition, snapshot).toOption.get)
  end instrument

  private def gridDefinition(assetName: String, gridName: String, quantum: Rational): GridDefinition =
    GridDefinition(
      GridIdentity(
        DimKey.atom(AtomId(s"asset:$assetName")),
        GridKey(GridId.from(gridName).toOption.get, GridVersion.from(1).toOption.get)
      ),
      PositiveRational(quantum).toOption.get
    )

  private def resolvedAsset(name: String, snapshot: CatalogSnapshot): Asset =
    snapshot.resolveAsset(AssetId.from(name).toOption.get).toOption.get

  private def resolvedGrid(asset: Asset, name: String, snapshot: CatalogSnapshot): GridHandle[asset.D] =
    snapshot
      .resolveGrid(asset.dimension)(GridKey(GridId.from(name).toOption.get, GridVersion.from(1).toOption.get))
      .toOption
      .get
end InstrumentFixtures
