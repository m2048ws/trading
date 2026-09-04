package external.economics.core

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

object PureCoreClient:
  def lots[I <: Instrument](instrument: I)(count: BigInt): Either[LotError, instrument.Lots] =
    Lots.fromCount(instrument)(count)

  def price[I <: Instrument](instrument: I)(
    rate: Rate[instrument.BaseD, instrument.QuoteD]
  ): Either[PriceError, instrument.Price] =
    Price.fromRate(instrument)(rate)

  def value[I <: Instrument](instrument: I)(
    position: instrument.PositionLots,
    state: instrument.MarketState
  ): Either[ValuationError, Quantity[instrument.SettleD]] =
    Valuation.positionValue(instrument)(position, state)

  def payoffs[I <: Instrument](instrument: I)(
    base: Rate[instrument.PositionD, instrument.BaseD],
    quote: Rate[instrument.PositionD, instrument.QuoteD]
  ): (Rate[instrument.PositionD, instrument.BaseD], Rate[instrument.PositionD, instrument.QuoteD]) =
    (base, quote)

  def positionViaRole[I <: Instrument](instrument: I)(
    value: Quantity[instrument.roles.position.D]
  ): Quantity[instrument.PositionD] =
    value

  def payoffsViaRoles[I <: Instrument](instrument: I)(
    base: Rate[instrument.roles.position.D, instrument.roles.base.D],
    quote: Rate[instrument.roles.position.D, instrument.roles.quote.D]
  ): (Rate[instrument.PositionD, instrument.BaseD], Rate[instrument.PositionD, instrument.QuoteD]) =
    (base, quote)

  def settlementViaRole[I <: Instrument](instrument: I)(
    value: Quantity[instrument.roles.settle.D]
  ): Quantity[instrument.SettleD] =
    value

  private def required[E, A](context: String, result: Either[E, A]): A =
    result.fold(error => throw new AssertionError(s"$context: $error"), identity)

  private val baseDefinition = AssetDefinition(
    required("base id", AssetId.from("core-base")),
    AtomId("core:base")
  )
  private val quoteDefinition = AssetDefinition(
    required("quote id", AssetId.from("core-quote")),
    AtomId("core:quote")
  )
  private val positionDefinition = AssetDefinition(
    required("position id", AssetId.from("core-position")),
    AtomId("core:position")
  )
  private val baseKey     = DimKey.atom(baseDefinition.dimensionAtom)
  private val quoteKey    = DimKey.atom(quoteDefinition.dimensionAtom)
  private val positionKey = DimKey.atom(positionDefinition.dimensionAtom)
  private val priceKey    = DimKey.multiply(quoteKey, DimKey.inverse(baseKey))
  private val lotsDefinition = GridDefinition(
    GridIdentity(
      positionKey,
      GridKey(required("lot grid id", GridId.from("core-lots")), required("lot grid version", GridVersion.from(1)))
    ),
    required("lot quantum", PositiveRational(Rational.one))
  )
  private val priceDefinition = GridDefinition(
    GridIdentity(
      priceKey,
      GridKey(
        required("price grid id", GridId.from("core-price")),
        required("price grid version", GridVersion.from(1))
      )
    ),
    required("price quantum", PositiveRational(Rational.one))
  )
  private val batch = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterDimension(priceKey),
    CatalogCommand.RegisterGrid(lotsDefinition),
    CatalogCommand.RegisterGrid(priceDefinition)
  )
  private val snapshot = required(
    "catalog commit",
    CatalogModel.commit(CatalogRoot.create().initialState, batch)
  ).state.snapshot
  private val base     = required("base asset", snapshot.resolveAsset(baseDefinition.id))
  private val quote    = required("quote asset", snapshot.resolveAsset(quoteDefinition.id))
  private val position = required("position asset", snapshot.resolveAsset(positionDefinition.id))
  private val lotsGrid = required("lot grid", snapshot.resolveGrid(position.dimension)(lotsDefinition.key))
  private val priceDimension = required("price dimension", snapshot.resolveDimension(priceKey))
  private val priceGrid = required("price grid", snapshot.resolveGrid(priceDimension)(priceDefinition.key))
  private val definition = InstrumentDefinition(
    InstrumentIdentity(
      required("instrument id", InstrumentId.from("core-instrument")),
      required("underlying id", UnderlyingId.from("core-underlying"))
    ),
    AssetRoleIds(base.id, quote.id, position.id, quote.id),
    ListingDefinition(lotsGrid.identity, priceGrid.identity),
    PayoffDefinition(Rational.one, Rational.zero)
  )
  val spec       = required("instrument assembly", InstrumentAssembler.assemble(definition, snapshot))
  val instrument = Instrument.fromSpec(spec)
  val concreteLots = required("lots", lots(instrument)(2))
  val concretePosition = PositionLots.fromCoordinate(instrument)(concreteLots.count.unrefined)
  val concretePrice    = required("price", Price.exact(instrument)(Rational(100)))
  val concreteState    = required("market state", MarketState.quoteSettled(instrument)(concretePrice))
  val concretePositionQuantity: Quantity[instrument.PositionD] =
    positionViaRole(instrument)(concretePosition.quantity)
  val concretePriceRate: Rate[instrument.BaseD, instrument.QuoteD] = concretePrice.rate
  val concretePayoffs =
    payoffs(instrument)(instrument.basePerPosition, instrument.quotePerPosition)
  val concreteRolePayoffs =
    payoffsViaRoles(instrument)(concretePayoffs._1, concretePayoffs._2)
  val concreteValue: Quantity[instrument.SettleD] =
    settlementViaRole(instrument)(required("position value", value(instrument)(concretePosition, concreteState)))

  private def rejectsSerialization(value: JavaSerializationUnsupported): Boolean =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      try
        output.writeObject(value)
        false
      catch case _: NotSerializableException => true
    finally output.close()

  def run(): Unit =
    assert(concreteLots.count.unrefined == BigInt(2))
    assert(concretePositionQuantity.coefficient == Rational(2))
    assert(concretePrice.coefficient == Rational(100))
    assert(concretePriceRate.coefficient == Rational(100))
    assert(concretePayoffs._1.coefficient == Rational.one)
    assert(concretePayoffs._2.coefficient == Rational.zero)
    assert(concreteRolePayoffs == concretePayoffs)
    assert(concreteValue.coefficient == Rational(200))
    Vector[JavaSerializationUnsupported](
      spec,
      instrument,
      concreteLots,
      concretePosition,
      concretePrice,
      concreteState
    ).foreach(value => assert(rejectsSerialization(value)))
end PureCoreClient
