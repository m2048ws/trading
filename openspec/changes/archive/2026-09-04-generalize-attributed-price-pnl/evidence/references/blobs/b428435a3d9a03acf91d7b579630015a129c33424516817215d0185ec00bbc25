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
    rate: Rate[instrument.roles.base.D, instrument.roles.quote.D]
  ): Either[PriceError, instrument.Price] =
    Price.fromRate(instrument)(rate)

  def value[I <: Instrument](instrument: I)(
    position: instrument.PositionLots,
    state: instrument.MarketState
  ): Either[ValuationError, Quantity[instrument.roles.settle.D]] =
    Valuation.positionValue(instrument)(position, state)

  def attributedPricePnl[I <: Instrument, A](instrument: I)(
    changes: Vector[instrument.AttributedPriceChange[A]],
    endpoint: instrument.PricePnlEndpoint
  ): Either[AttributedPricePnlErrors, instrument.AttributedPricePnl[A]] =
    AttributedPricePnl.calculate(instrument)(changes, endpoint)

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
  val concreteValue    = required("position value", value(instrument)(concretePosition, concreteState))
  val closingPosition  = PositionLots.fromCoordinate(instrument)(BigInt(-2))
  val exitPrice        = required("exit price", Price.exact(instrument)(Rational(110)))
  val exitState        = required("exit market state", MarketState.quoteSettled(instrument)(exitPrice))
  val attributedChanges: Vector[instrument.AttributedPriceChange[String]] = Vector(
    AttributedPriceChange("entry", concretePosition, concreteState),
    AttributedPriceChange("exit", closingPosition, exitState)
  )
  val attributedResult = required(
    "attributed price PnL",
    attributedPricePnl(instrument)(attributedChanges, PricePnlEndpoint.Flat)
  )
  val nonFlatViolation =
    attributedPricePnl(instrument)(Vector(attributedChanges.head), PricePnlEndpoint.Flat) match
      case Left(errors) => errors.head
      case Right(value) => throw new AssertionError(s"non-flat calculation unexpectedly succeeded: $value")

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
    assert(concretePrice.coefficient == Rational(100))
    assert(concreteValue.coefficient == Rational(200))
    assert(attributedResult.instrumentId == instrument.identity.id)
    assert(attributedResult.settlement.id == instrument.roles.settle.id)
    assert(attributedResult.endingPosition.coordinate == BigInt(0))
    assert(attributedResult.settledContributions.map(_.attribution) == Vector("entry", "exit"))
    assert(attributedResult.settledContributions.map(_.positionChange.coordinate) == Vector(BigInt(2), BigInt(-2)))
    assert(
      attributedResult.settledContributions.map(_.quantity.coefficient) ==
        Vector(Rational(-200), Rational(220))
    )
    assert(attributedResult.settledContributions.map(_.original) == attributedChanges)
    assert(attributedResult.pricePnl.quantity.coefficient == Rational(20))
    attributedResult.endpoint match
      case PricePnlEndpoint.Flat      => ()
      case PricePnlEndpoint.Marked(_) => throw new AssertionError("flat calculation retained a mark")
    assert(nonFlatViolation == AttributedPricePnlViolation.NonFlatPositionRequiresMark(BigInt(2)))
    (
      Vector[JavaSerializationUnsupported](
        spec,
        instrument,
        concreteLots,
        concretePosition,
        concretePrice,
        concreteState,
        attributedResult
      ) ++ attributedChanges ++ attributedResult.settledContributions
    ).foreach(value => assert(rejectsSerialization(value)))
end PureCoreClient
