package trading.economics

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy

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

    assertEquals(instrument.priceCoordinate(instrument.priceExactly(onGrid).toOption.get), BigInt(3))
    assert(instrument.priceExactly(offGrid).isLeft)
    assert(instrument.price(0).isLeft)

    val (selected, residual) = instrument.quantizePrice(offGrid, QuantizationPolicy.Floor).toOption.get
    assertEquals(instrument.priceCoordinate(selected), BigInt(2))
    assertEquals(residual.coefficient, Rational(1, 4))

  test("quote settlement and explicit additional conversions are exact and type-safe"):
    val instrument = fixture.linear
    val price      = fixture.price(instrument, 10_000)
    val tokenToUsd =
      SettlementConversion
        .positive(fixture.token, fixture.usd)(
          Rate(fixture.token.dimension.asDimensionRef, fixture.usd.dimension.asDimensionRef, Rational(2))
        )
        .toOption
        .get
    val state  = instrument.marketStateForQuote(price, Vector(tokenToUsd)).toOption.get
    val tokens = Quantity(fixture.token.dimension.asDimensionRef, Rational(3, 2))

    assertEquals(state.conversions.sources.toSet, Set(fixture.btc.id, fixture.usd.id, fixture.token.id))
    assertEquals(
      instrument.convertToSettle(state.conversions, fixture.token)(tokens).toOption.get.coefficient,
      Rational(3)
    )

  test("dual-anchor market construction rejects exact incoherence"):
    val instrument = fixture.quanto
    val price      = fixture.price(instrument, 100)
    val baseAnchor =
      Rate(instrument.base.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, Rational(60))
    val quoteAnchor =
      Rate(instrument.quote.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, Rational(1, 2))

    assert(instrument.marketStateChecked(price, baseAnchor, quoteAnchor).isLeft)
    val coherent =
      Rate(instrument.base.dimension.asDimensionRef, instrument.settle.dimension.asDimensionRef, Rational(50))
    assert(instrument.marketStateChecked(price, coherent, quoteAnchor).isRight)

  test("dual-anchor market construction rejects nonidentity settlement conversions truthfully"):
    val quoteSettled = fixture.linear
    val quotePrice   = fixture.price(quoteSettled, 100)
    val quoteBase    =
      Rate(quoteSettled.base.dimension.asDimensionRef, quoteSettled.settle.dimension.asDimensionRef, Rational(200))
    val nonidentityQuote =
      Rate(quoteSettled.quote.dimension.asDimensionRef, quoteSettled.settle.dimension.asDimensionRef, Rational(2))

    assertEquals(
      quoteSettled.marketStateChecked(quotePrice, quoteBase, nonidentityQuote),
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
    assert(quoteSettled.marketStateChecked(quotePrice, quotedBase, quoteIdentity).isRight)

    val baseSettled     = fixture.inverse
    val basePrice       = fixture.price(baseSettled, 100)
    val nonidentityBase =
      Rate(baseSettled.base.dimension.asDimensionRef, baseSettled.settle.dimension.asDimensionRef, Rational(2))
    val baseQuote =
      Rate(baseSettled.quote.dimension.asDimensionRef, baseSettled.settle.dimension.asDimensionRef, Rational(1, 50))

    assertEquals(
      baseSettled.marketStateChecked(basePrice, nonidentityBase, baseQuote),
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
    assert(baseSettled.marketStateChecked(basePrice, baseIdentity, reciprocalQuote).isRight)

  test("universal valuation includes lots and is symmetric for long and short"):
    val instrument = fixture.linear
    val lots       = instrument.lots(1000).toOption.get
    val entry      = instrument.marketStateForQuote(fixture.price(instrument, 100)).toOption.get
    val exit       = instrument.marketStateForQuote(fixture.price(instrument, 110)).toOption.get
    val long       = instrument.positionLots(Side.Buy, lots)
    val short      = instrument.positionLots(Side.Sell, lots)

    assertEquals(instrument.positionValue(long, entry).coefficient, Rational(100))
    assertEquals(instrument.pricePnl(long, entry, exit).coefficient, Rational(10))
    assertEquals(instrument.pricePnl(short, entry, exit).coefficient, Rational(-10))

  test("inverse valuation retains an off-grid exact rational result"):
    val instrument = fixture.inverse
    val lots       = instrument.lots(1000).toOption.get
    val entry      = instrument.marketStateForBase(fixture.price(instrument, 3)).toOption.get
    val exit       = instrument.marketStateForBase(fixture.price(instrument, 7)).toOption.get
    val held       = instrument.positionLots(Side.Buy, lots)

    assertEquals(instrument.pricePnl(held, entry, exit).coefficient, Rational(400, 21))

  test("spot-like, linear, inverse, and quanto-style definitions share one valuation API"):
    val spot         = fixture.spotLike
    val spotLots     = spot.lots(100_000_000).toOption.get
    val spotState    = spot.marketStateForQuote(fixture.price(spot, 20_000)).toOption.get
    val linear       = fixture.linear
    val linearLots   = linear.lots(1000).toOption.get
    val linearState  = linear.marketStateForQuote(fixture.price(linear, 20_000)).toOption.get
    val inverse      = fixture.inverse
    val inverseLots  = inverse.lots(1000).toOption.get
    val inverseState = inverse.marketStateForBase(fixture.price(inverse, 20_000)).toOption.get
    val quanto       = fixture.quanto
    val quantoLots   = quanto.lots(1000).toOption.get
    val quoteToEur   =
      Rate(quanto.quote.dimension.asDimensionRef, quanto.settle.dimension.asDimensionRef, Rational(1, 2))
    val quantoState = quanto.marketStateFromQuote(fixture.price(quanto, 20_000), quoteToEur).toOption.get

    assertEquals(spot.positionValue(spot.positionLots(Side.Buy, spotLots), spotState).coefficient, Rational(20_000))
    assertEquals(
      linear.positionValue(linear.positionLots(Side.Buy, linearLots), linearState).coefficient,
      Rational(20_000)
    )
    assertEquals(
      inverse.positionValue(inverse.positionLots(Side.Buy, inverseLots), inverseState).coefficient,
      Rational(-1, 200)
    )
    assertEquals(
      quanto.positionValue(quanto.positionLots(Side.Buy, quantoLots), quantoState).coefficient,
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
