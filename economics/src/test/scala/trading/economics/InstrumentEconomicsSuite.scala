package trading.economics

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole

class InstrumentEconomicsSuite extends FunSuite:
  private val fixture = new EconomicsFixtures

  test("positive lots include their exact lot quantum and side determines signed position"):
    val instrument = fixture.linear
    val lots       = instrument.lots(1000).toOption.get

    assertEquals(instrument.lotCount(lots), BigInt(1000))
    assertEquals(instrument.lotsQuantity(lots).coefficient, Rational.one)
    assertEquals(instrument.positionLotCount(instrument.positionLots(Side.Buy, lots)), BigInt(1000))
    assertEquals(instrument.positionLotCount(instrument.positionLots(Side.Sell, lots)), BigInt(-1000))
    assertEquals(instrument.positionLotCount(instrument.flatPosition), BigInt(0))
    assert(instrument.lots(0).isLeft)
    assert(instrument.lots(-1).isLeft)

  test("price construction is positive, exact-grid checked, and explicitly residual-bearing"):
    val instrument = fixture.linear
    val onGrid     =
      Rate(instrument.base.dimension.asDimensionRef, instrument.quote.dimension.asDimensionRef, Rational(3, 2))
    val offGrid =
      Rate(instrument.base.dimension.asDimensionRef, instrument.quote.dimension.asDimensionRef, Rational(5, 4))

    assertEquals(instrument.prices.ticks(instrument.prices.fromRate(onGrid).toOption.get), BigInt(3))
    assert(instrument.prices.fromRate(offGrid).isLeft)
    assert(instrument.prices.exact(Rational.zero).isLeft)

    val (selected, residual) = instrument.prices.quantizeRate(offGrid, QuantizationPolicy.Floor).toOption.get
    assertEquals(instrument.prices.ticks(selected), BigInt(2))
    assertEquals(residual.coefficient, Rational(1, 4))

  test("scalar and typed price paths are observationally equivalent"):
    val instrument  = fixture.linear
    val onGrid      = Rational(3, 2)
    val offGrid     = Rational(5, 4)
    val typedOnGrid =
      Rate(instrument.base.dimension.asDimensionRef, instrument.quote.dimension.asDimensionRef, onGrid)
    val typedOffGrid =
      Rate(instrument.base.dimension.asDimensionRef, instrument.quote.dimension.asDimensionRef, offGrid)

    val scalar = instrument.prices.exact(onGrid).toOption.get
    val typed  = instrument.prices.fromRate(typedOnGrid).toOption.get
    assertEquals(instrument.prices.ticks(scalar), instrument.prices.ticks(typed))
    assertEquals(instrument.prices.coefficient(scalar), onGrid)
    assertEquals(instrument.prices.rate(scalar).coefficient, onGrid)
    assertEquals(instrument.prices.exact(offGrid), instrument.prices.fromRate(typedOffGrid))

    val scalarQuantized = instrument.prices.quantize(offGrid, QuantizationPolicy.Floor).toOption.get
    val typedQuantized  = instrument.prices.quantizeRate(typedOffGrid, QuantizationPolicy.Floor).toOption.get
    assertEquals(instrument.prices.ticks(scalarQuantized._1), instrument.prices.ticks(typedQuantized._1))
    assertEquals(scalarQuantized._2.coefficient, typedQuantized._2.coefficient)

    val reconstructed = instrument.prices.fromTicks(PositiveWhole(3).toOption.get)
    assertEquals(instrument.prices.ticks(reconstructed), BigInt(3))
    assertEquals(instrument.prices.coefficient(reconstructed), onGrid)

  test("scalar and typed settlement conversions share validation and diagnostics"):
    val typed = Rate(fixture.token.dimension.asDimensionRef, fixture.usd.dimension.asDimensionRef, Rational(2))
    val scalarConversion = SettlementConversion.positive(fixture.token, fixture.usd, Rational(2)).toOption.get
    val typedConversion  = SettlementConversion.fromRate(fixture.token, fixture.usd)(typed).toOption.get

    assertEquals(scalarConversion.source.id, typedConversion.source.id)
    assertEquals(scalarConversion.target.id, typedConversion.target.id)
    assertEquals(scalarConversion.coefficient, typedConversion.coefficient)

    val invalidScalar = SettlementConversion.positive(fixture.usd, fixture.usd, Rational(2))
    val invalidTyped  = SettlementConversion.fromRate(fixture.usd, fixture.usd)(
      Rate(fixture.usd.dimension.asDimensionRef, fixture.usd.dimension.asDimensionRef, Rational(2))
    )
    assertEquals(invalidScalar, invalidTyped)

  test("scalar and typed market anchors share construction and failure behavior"):
    val instrument       = fixture.quanto
    val price            = fixture.price(instrument, 100)
    val quoteCoefficient = Rational(1, 2)
    val quoteRate        =
      Rate(instrument.quote.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, quoteCoefficient)

    val scalar = instrument.market.fromQuoteAnchor(price, quoteCoefficient).toOption.get
    val typed  = instrument.market.fromQuoteRate(price, quoteRate).toOption.get
    assertEquals(scalar.conversions.sources, typed.conversions.sources)
    assertEquals(
      instrument.valuation.settlePerPosition(scalar).coefficient,
      instrument.valuation.settlePerPosition(typed).coefficient
    )

    val coherentBase  = Rational(50)
    val scalarChecked = instrument.market.fromAnchors(price, coherentBase, quoteCoefficient).toOption.get
    val typedChecked  = instrument.market.fromRates(
      price,
      Rate(instrument.base.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, coherentBase),
      quoteRate
    ).toOption.get
    assertEquals(scalarChecked.conversions.sources, typedChecked.conversions.sources)

    val invalidScalar = instrument.market.fromAnchors(price, Rational(60), quoteCoefficient)
    val invalidTyped  = instrument.market.fromRates(
      price,
      Rate(instrument.base.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, Rational(60)),
      quoteRate
    )
    assertEquals(invalidScalar, invalidTyped)

  test("quote settlement and explicit additional conversions are exact and type-safe"):
    val instrument = fixture.linear
    val price      = fixture.price(instrument, 10_000)
    val tokenToUsd =
      SettlementConversion
        .fromRate(fixture.token, fixture.usd)(
          Rate(fixture.token.dimension.asDimensionRef, fixture.usd.dimension.asDimensionRef, Rational(2))
        )
        .toOption
        .get
    val state  = instrument.market.quoteSettled(price, Vector(tokenToUsd)).toOption.get
    val tokens = Quantity(fixture.token.dimension.asDimensionRef, Rational(3, 2))

    assertEquals(state.conversions.sources.toSet, Set(fixture.btc.id, fixture.usd.id, fixture.token.id))
    assertEquals(
      instrument.market.convertToSettle(fixture.token, state.conversions)(tokens).toOption.get.coefficient,
      Rational(3)
    )

  test("market validates zero or many direct additional conversions as one aggregate"):
    val instrument = fixture.linear
    val price      = fixture.price(instrument, 10_000)
    val tokenToUsd = SettlementConversion.positive(fixture.token, fixture.usd, Rational(2)).toOption.get
    val eurToUsd   = SettlementConversion.positive(fixture.eur, fixture.usd, Rational(6, 5)).toOption.get

    val empty = instrument.market.quoteSettled(price).toOption.get
    assertEquals(empty.conversions.sources.toSet, Set(fixture.btc.id, fixture.usd.id))

    val many = instrument.market.quoteSettled(price, Vector(tokenToUsd, eurToUsd)).toOption.get
    assertEquals(
      many.conversions.sources.toSet,
      Set(fixture.btc.id, fixture.usd.id, fixture.token.id, fixture.eur.id)
    )
    assertEquals(
      instrument.market.quoteSettled(price, Vector(tokenToUsd, tokenToUsd)),
      Left(DuplicateConversion(fixture.token.id))
    )

  test("dual-anchor market construction rejects exact incoherence"):
    val instrument = fixture.quanto
    val price      = fixture.price(instrument, 100)
    val baseAnchor =
      Rate(instrument.base.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, Rational(60))
    val quoteAnchor =
      Rate(instrument.quote.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, Rational(1, 2))

    assert(instrument.market.fromRates(price, baseAnchor, quoteAnchor).isLeft)
    val coherent =
      Rate(instrument.base.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, Rational(50))
    assert(instrument.market.fromRates(price, coherent, quoteAnchor).isRight)

  test("dual-anchor market construction rejects nonidentity settlement conversions truthfully"):
    val quoteSettled = fixture.linear
    val quotePrice   = fixture.price(quoteSettled, 100)
    val quoteBase    =
      Rate(quoteSettled.base.dimension.asDimensionRef, quoteSettled.settle.dimension.asDimensionRef, Rational(200))
    val nonidentityQuote =
      Rate(quoteSettled.quote.dimension.asDimensionRef, quoteSettled.settle.dimension.asDimensionRef, Rational(2))

    assertEquals(
      quoteSettled.market.fromRates(quotePrice, quoteBase, nonidentityQuote),
      Left(
        InvalidConversion(
          quoteSettled.quote.id,
          quoteSettled.settle.id,
          Rational(2),
          "settlement identity conversion must equal one"
        )
      )
    )
    val quoteIdentity =
      Rate(quoteSettled.quote.dimension.asDimensionRef, quoteSettled.settle.dimension.asDimensionRef, Rational.one)
    val quotedBase =
      Rate(quoteSettled.base.dimension.asDimensionRef, quoteSettled.settle.dimension.asDimensionRef, Rational(100))
    assert(quoteSettled.market.fromRates(quotePrice, quotedBase, quoteIdentity).isRight)

    val baseSettled     = fixture.inverse
    val basePrice       = fixture.price(baseSettled, 100)
    val nonidentityBase =
      Rate(baseSettled.base.dimension.asDimensionRef, baseSettled.settle.dimension.asDimensionRef, Rational(2))
    val baseQuote =
      Rate(baseSettled.quote.dimension.asDimensionRef, baseSettled.settle.dimension.asDimensionRef, Rational(1, 50))

    assertEquals(
      baseSettled.market.fromRates(basePrice, nonidentityBase, baseQuote),
      Left(
        InvalidConversion(
          baseSettled.base.id,
          baseSettled.settle.id,
          Rational(2),
          "settlement identity conversion must equal one"
        )
      )
    )
    val baseIdentity =
      Rate(baseSettled.base.dimension.asDimensionRef, baseSettled.settle.dimension.asDimensionRef, Rational.one)
    val reciprocalQuote =
      Rate(baseSettled.quote.dimension.asDimensionRef, baseSettled.settle.dimension.asDimensionRef, Rational(1, 100))
    assert(baseSettled.market.fromRates(basePrice, baseIdentity, reciprocalQuote).isRight)

  test("universal valuation includes lots and is symmetric for long and short"):
    val instrument = fixture.linear
    val lots       = instrument.lots(1000).toOption.get
    val entry      = instrument.market.quoteSettled(fixture.price(instrument, 100)).toOption.get
    val exit       = instrument.market.quoteSettled(fixture.price(instrument, 110)).toOption.get
    val long       = instrument.positionLots(Side.Buy, lots)
    val short      = instrument.positionLots(Side.Sell, lots)

    assertEquals(instrument.valuation.positionValue(long, entry).coefficient, Rational(100))
    assertEquals(instrument.valuation.pricePnl(long, entry, exit).coefficient, Rational(10))
    assertEquals(instrument.valuation.pricePnl(short, entry, exit).coefficient, Rational(-10))

  test("inverse valuation retains an off-grid exact rational result"):
    val instrument = fixture.inverse
    val lots       = instrument.lots(1000).toOption.get
    val entry      = instrument.market.baseSettled(fixture.price(instrument, 3)).toOption.get
    val exit       = instrument.market.baseSettled(fixture.price(instrument, 7)).toOption.get
    val held       = instrument.positionLots(Side.Buy, lots)

    assertEquals(instrument.valuation.pricePnl(held, entry, exit).coefficient, Rational(400, 21))

  test("spot-like, linear, inverse, and quanto-style definitions share one valuation API"):
    val spot         = fixture.spotLike
    val spotLots     = spot.lots(100_000_000).toOption.get
    val spotState    = spot.market.quoteSettled(fixture.price(spot, 20_000)).toOption.get
    val linear       = fixture.linear
    val linearLots   = linear.lots(1000).toOption.get
    val linearState  = linear.market.quoteSettled(fixture.price(linear, 20_000)).toOption.get
    val inverse      = fixture.inverse
    val inverseLots  = inverse.lots(1000).toOption.get
    val inverseState = inverse.market.baseSettled(fixture.price(inverse, 20_000)).toOption.get
    val quanto       = fixture.quanto
    val quantoLots   = quanto.lots(1000).toOption.get
    val quoteToEur   =
      Rate(quanto.quote.dimension.asDimensionRef, quanto.settle.dimension.asDimensionRef, Rational(1, 2))
    val quantoState = quanto.market.fromQuoteRate(fixture.price(quanto, 20_000), quoteToEur).toOption.get

    assertEquals(spot.valuation.positionValue(spot.positionLots(Side.Buy, spotLots), spotState).coefficient,
      Rational(20_000))
    assertEquals(
      linear.valuation.positionValue(linear.positionLots(Side.Buy, linearLots), linearState).coefficient,
      Rational(20_000)
    )
    assertEquals(
      inverse.valuation.positionValue(inverse.positionLots(Side.Buy, inverseLots), inverseState).coefficient,
      Rational(-1, 200)
    )
    assertEquals(
      quanto.valuation.positionValue(quanto.positionLots(Side.Buy, quantoLots), quantoState).coefficient,
      Rational(10_000)
    )

  test("instrument construction rejects empty payoffs and mismatched grids"):
    val empty = Instrument.create(
      InstrumentId("empty"),
      UnderlyingId("empty"),
      fixture.btc,
      fixture.usd,
      fixture.contract,
      fixture.usd
    )(
      fixture.contractLots,
      fixture.usdPerBtcTicks,
      Rate(fixture.contract.dimension.asDimensionRef, fixture.btc.dimension.asDimensionRef, Rational.zero),
      Rate(fixture.contract.dimension.asDimensionRef, fixture.usd.dimension.asDimensionRef, Rational.zero)
    )

    assertEquals(empty, Left(EmptyContractPayoff(InstrumentId("empty"))))
    val wrongGrid = Instrument.create(
      InstrumentId("wrong-grid"),
      UnderlyingId("wrong-grid"),
      fixture.btc,
      fixture.usd,
      fixture.contract,
      fixture.usd
    )(
      fixture.usdCents,
      fixture.usdPerBtcTicks,
      Rate(fixture.contract.dimension.asDimensionRef, fixture.btc.dimension.asDimensionRef, Rational.one),
      Rate(fixture.contract.dimension.asDimensionRef, fixture.usd.dimension.asDimensionRef, Rational.zero)
    )
    assert(wrongGrid.isLeft)

end InstrumentEconomicsSuite
