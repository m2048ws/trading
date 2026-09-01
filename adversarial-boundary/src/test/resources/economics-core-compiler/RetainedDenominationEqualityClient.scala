package external.economics.core

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

object RetainedDenominationEqualityClient:
  private def required[E, A](context: String, result: Either[E, A]): A =
    result.fold(error => throw new AssertionError(s"$context: $error"), identity)

  private val baseDefinition = AssetDefinition(
    required("base id", AssetId.from("equality-base")),
    AtomId("equality:base")
  )
  private val quoteDefinition = AssetDefinition(
    required("quote id", AssetId.from("equality-quote")),
    AtomId("equality:quote")
  )
  private val positionDefinition = AssetDefinition(
    required("position id", AssetId.from("equality-position")),
    AtomId("equality:position")
  )
  private val baseKey     = DimKey.atom(baseDefinition.dimensionAtom)
  private val quoteKey    = DimKey.atom(quoteDefinition.dimensionAtom)
  private val positionKey = DimKey.atom(positionDefinition.dimensionAtom)
  private val priceKey    = DimKey.multiply(quoteKey, DimKey.inverse(baseKey))
  private val lotsIdentity = GridIdentity(
    positionKey,
    GridKey(required("lot grid id", GridId.from("equality-lots")), required("lot grid version", GridVersion.from(1)))
  )
  private val priceIdentity = GridIdentity(
    priceKey,
    GridKey(
      required("price grid id", GridId.from("equality-price")),
      required("price grid version", GridVersion.from(1))
    )
  )
  private val feeIdentity = GridIdentity(
    quoteKey,
    GridKey(required("fee grid id", GridId.from("equality-fee")), required("fee grid version", GridVersion.from(1)))
  )
  private val instrumentIdentity = InstrumentIdentity(
    required("instrument id", InstrumentId.from("equality-instrument")),
    required("underlying id", UnderlyingId.from("equality-underlying"))
  )
  private val kind = required("fee kind", FeeKind.from("equality-fee"))

  private final class Model(val quote: Asset)(val feeGrid: GridHandle[quote.D], val instrument: Instrument):
    val denomination = required(
      "fee denomination",
      FeeDenomination.create(instrument)(quote, feeGrid, QuantizationPolicy.Floor)
    )
    private val zero     = Quantity(quote.dimension.ref, Rational.zero)
    val fee              = required("fee", Fee.create(instrument)(denomination, kind, zero))
    val repeatedFee      = required("repeated fee", Fee.create(instrument)(denomination, kind, zero))
    val position         = PositionLots.flat(instrument)
    val repeatedPosition = PositionLots.flat(instrument)
    val price            = required("price", Price.exact(instrument)(Rational(100)))
    val repeatedPrice    = required("repeated price", Price.exact(instrument)(Rational(100)))
    private val market = required(
      "market state",
      MarketState.quoteSettled(instrument)(price)
    )
    val pricePnl = required(
      "price pnl",
      PricePnl.calculate(instrument)(position, market, market)
    )
    val repeatedPricePnl = required(
      "repeated price pnl",
      PricePnl.calculate(instrument)(repeatedPosition, market, market)
    )
    val contribution = required(
      "settled contribution",
      SettledFeeContribution.convert(instrument)(fee, market)
    )
    val repeatedContribution = required(
      "repeated settled contribution",
      SettledFeeContribution.convert(instrument)(repeatedFee, market)
    )
    val pnl         = required("pnl", Pnl.create(instrument)(pricePnl, Vector(contribution)))
    val repeatedPnl = required("repeated pnl", Pnl.create(instrument)(pricePnl, Vector(repeatedContribution)))
  end Model

  private def model(feeQuantum: Rational): Model =
    val lotsDefinition = GridDefinition(lotsIdentity, required("lot quantum", PositiveRational(Rational.one)))
    val priceDefinition = GridDefinition(priceIdentity, required("price quantum", PositiveRational(Rational.one)))
    val feeDefinition   = GridDefinition(feeIdentity, required("fee quantum", PositiveRational(feeQuantum)))
    val batch = CatalogBatch.of(
      CatalogCommand.RegisterAsset(baseDefinition),
      CatalogCommand.RegisterAsset(quoteDefinition),
      CatalogCommand.RegisterAsset(positionDefinition),
      CatalogCommand.RegisterDimension(priceKey),
      CatalogCommand.RegisterGrid(lotsDefinition),
      CatalogCommand.RegisterGrid(priceDefinition),
      CatalogCommand.RegisterGrid(feeDefinition)
    )
    val snapshot = required(
      "catalog commit",
      CatalogModel.commit(CatalogRoot.create().initialState, batch)
    ).state.snapshot
    val base     = required("base asset", snapshot.resolveAsset(baseDefinition.id))
    val quote    = required("quote asset", snapshot.resolveAsset(quoteDefinition.id))
    val position = required("position asset", snapshot.resolveAsset(positionDefinition.id))
    val lotsGrid = required("lot grid", snapshot.resolveGrid(position.dimension)(lotsIdentity.key))
    val priceDimension = required("price dimension", snapshot.resolveDimension(priceKey))
    val priceGrid       = required("price grid", snapshot.resolveGrid(priceDimension)(priceIdentity.key))
    val feeGrid         = required("fee grid", snapshot.resolveGrid(quote.dimension)(feeIdentity.key))
    val definition = InstrumentDefinition(
      instrumentIdentity,
      AssetRoleIds(base.id, quote.id, position.id, quote.id),
      ListingDefinition(lotsGrid.identity, priceGrid.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )
    val instrument = Instrument.fromSpec(
      required("instrument assembly", InstrumentAssembler.assemble(definition, snapshot))
    )
    new Model(quote)(feeGrid, instrument)
  end model

  def run(): Unit =
    val cents           = model(Rational(1, 100))
    val repeatedCents   = model(Rational(1, 100))
    val fractionalCents = model(Rational(1, 1000))

    assert(cents.fee == cents.repeatedFee)
    assert(cents.fee.hashCode == cents.repeatedFee.hashCode)
    assert(cents.position == cents.repeatedPosition)
    assert(cents.position.hashCode == cents.repeatedPosition.hashCode)
    assert(cents.price == cents.repeatedPrice)
    assert(cents.price.hashCode == cents.repeatedPrice.hashCode)
    assert(cents.pricePnl == cents.repeatedPricePnl)
    assert(cents.pricePnl.hashCode == cents.repeatedPricePnl.hashCode)
    assert(cents.contribution == cents.repeatedContribution)
    assert(cents.contribution.hashCode == cents.repeatedContribution.hashCode)
    assert(cents.pnl == cents.repeatedPnl)
    assert(cents.pnl.hashCode == cents.repeatedPnl.hashCode)

    assert(Asset.reconcile(cents.quote, repeatedCents.quote).isLeft)
    assert(GridHandle.reconcile(cents.feeGrid, repeatedCents.feeGrid).isLeft)
    assert(cents.feeGrid.quantum.unrefined == repeatedCents.feeGrid.quantum.unrefined)
    assert(cents.position != repeatedCents.position)
    assert(cents.price != repeatedCents.price)
    assert(cents.pricePnl != repeatedCents.pricePnl)
    assert(cents.contribution != repeatedCents.contribution)

    assert(Asset.reconcile(cents.quote, fractionalCents.quote).isLeft)
    assert(GridHandle.reconcile(cents.feeGrid, fractionalCents.feeGrid).isLeft)
    assert(cents.feeGrid.quantum.unrefined != fractionalCents.feeGrid.quantum.unrefined)

    val provenanceFeesEqual = cents.fee == repeatedCents.fee
    val provenancePnlsEqual = cents.pnl == repeatedCents.pnl
    val quantumFeesEqual    = cents.fee == fractionalCents.fee
    val quantumFeeHashesEqual = cents.fee.hashCode == fractionalCents.fee.hashCode
    val quantumPnlsEqual      = cents.pnl == fractionalCents.pnl
    val quantumPnlHashesEqual = cents.pnl.hashCode == fractionalCents.pnl.hashCode
    assert(
      !provenanceFeesEqual && !provenancePnlsEqual && !quantumFeesEqual && !quantumFeeHashesEqual &&
        !quantumPnlsEqual && !quantumPnlHashesEqual,
      s"provenanceFeesEqual=$provenanceFeesEqual, provenancePnlsEqual=$provenancePnlsEqual, " +
        s"quantumFeesEqual=$quantumFeesEqual, quantumFeeHashesEqual=$quantumFeeHashesEqual, " +
        s"quantumPnlsEqual=$quantumPnlsEqual, quantumPnlHashesEqual=$quantumPnlHashesEqual"
    )
  end run
end RetainedDenominationEqualityClient
