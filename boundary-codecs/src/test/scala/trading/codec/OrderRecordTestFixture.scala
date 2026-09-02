package trading.codec

import trading.economics.instrument.AssetRoleIds
import trading.economics.instrument.Instrument
import trading.economics.instrument.InstrumentAssembler
import trading.economics.instrument.InstrumentDefinition
import trading.economics.instrument.InstrumentId
import trading.economics.instrument.InstrumentIdentity
import trading.economics.instrument.ListingDefinition
import trading.economics.instrument.Lots
import trading.economics.instrument.PayoffDefinition
import trading.economics.instrument.Price
import trading.economics.instrument.UnderlyingId
import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.quantity.refinement.PositiveRational
import trading.quantity.refinement.PositiveWhole
import trading.reference.AssetDefinition
import trading.reference.AssetId
import trading.reference.CatalogBatch
import trading.reference.CatalogCommand
import trading.reference.CatalogModel
import trading.reference.CatalogRoot
import trading.reference.GridDefinition
import trading.reference.GridId
import trading.reference.GridIdentity
import trading.reference.GridKey
import trading.reference.GridVersion

private[codec] final class OrderRecordTestFixture(prefix: String):
  private val baseDefinition     = asset("base")
  private val quoteDefinition    = asset("quote")
  private val positionDefinition = asset("position")
  private val settleDefinition   = asset("settle")
  private val tokenDefinition    = asset("token")
  private val rebateDefinition   = asset("rebate")

  private val positionDimension = DimKey.atom(positionDefinition.dimensionAtom)
  private val baseDimension     = DimKey.atom(baseDefinition.dimensionAtom)
  private val quoteDimension    = DimKey.atom(quoteDefinition.dimensionAtom)
  private val priceDimension    = DimKey.multiply(quoteDimension, DimKey.inverse(baseDimension))
  private val positionGrid      = grid(positionDimension, "position-lots", Rational(1, 1_000))
  private val priceGrid         = grid(priceDimension, "quote-per-base", Rational(1, 2))
  private val batch             = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterAsset(settleDefinition),
    CatalogCommand.RegisterAsset(tokenDefinition),
    CatalogCommand.RegisterAsset(rebateDefinition),
    CatalogCommand.RegisterDimension(priceDimension),
    CatalogCommand.RegisterGrid(positionGrid),
    CatalogCommand.RegisterGrid(priceGrid)
  )
  val snapshot = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get.state.snapshot

  val token  = snapshot.resolveAsset(tokenDefinition.id).toOption.get
  val rebate = snapshot.resolveAsset(rebateDefinition.id).toOption.get

  val instrument: Instrument = build("primary")
  val foreign: Instrument    = build("foreign")

  def lots[I <: Instrument](value: I, coordinate: BigInt): value.Lots =
    Lots.fromCount(value)(coordinate).toOption.get

  def price[I <: Instrument](value: I, coordinate: BigInt): value.Price =
    Price.fromTicks(value)(PositiveWhole(coordinate).toOption.get)

  private def build(name: String): Instrument =
    val definition = InstrumentDefinition(
      InstrumentIdentity(
        InstrumentId.from(s"$prefix-$name").toOption.get,
        UnderlyingId.from(s"$prefix-underlying-$name").toOption.get
      ),
      AssetRoleIds(baseDefinition.id, quoteDefinition.id, positionDefinition.id, settleDefinition.id),
      ListingDefinition(positionGrid.identity, priceGrid.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )
    Instrument.fromSpec(InstrumentAssembler.assemble(definition, snapshot).toOption.get)

  private def asset(name: String): AssetDefinition =
    AssetDefinition(AssetId.from(s"$prefix-$name").toOption.get, AtomId(s"$prefix:$name"))

  private def grid(dimension: DimKey, name: String, quantum: Rational): GridDefinition =
    GridDefinition(
      GridIdentity(
        dimension,
        GridKey(
          GridId.from(s"$prefix-$name").toOption.get,
          GridVersion.from(1).toOption.get
        )
      ),
      PositiveRational(quantum).toOption.get
    )
end OrderRecordTestFixture
