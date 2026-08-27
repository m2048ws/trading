package trading.economics.instrument

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

class InstrumentEconomicsSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  test("owned lots and signed positions retain refined value-local observations"):
    val lots  = instrument.lots(1000).toOption.get
    val long  = instrument.positionLots(Side.Buy, lots).toOption.get
    val short = instrument.positionLots(Side.Sell, lots).toOption.get

    assertEquals(lots.count.unrefined, BigInt(1000))
    assertEquals(lots.quantity.coefficient, Rational.one)
    assertEquals(long.count, BigInt(1000))
    assertEquals(short.count, BigInt(-1000))
    assertEquals(instrument.flatPosition.count, BigInt(0))
    assertEquals(instrument.lots(0), Left(InvalidLots(0)))
    assertEquals(instrument.lots(-1), Left(InvalidLots(-1)))

  test("exact and quantized price construction preserve positivity, grid membership, and residual"):
    val exact = instrument.prices.exact(Rational(3, 2)).toOption.get
    assertEquals(exact.ticks.unrefined, BigInt(3))
    assertEquals(exact.coefficient, Rational(3, 2))
    assertEquals(exact.rate.coefficient, Rational(3, 2))
    assert(instrument.prices.exact(Rational(5, 4)).isLeft)
    assertEquals(instrument.prices.exact(Rational.zero), Left(InvalidPriceCoordinate(0)))

    val (selected, residual) = instrument.prices.quantize(Rational(5, 4), QuantizationPolicy.Floor).toOption.get
    assertEquals(selected.ticks.unrefined, BigInt(2))
    assertEquals(selected.coefficient + residual.coefficient, Rational(5, 4))

    val typed = Rate(
      instrument.roles.base.dimension.ref,
      instrument.roles.quote.dimension.ref,
      Rational(3, 2)
    )
    assertEquals(instrument.prices.fromRate(typed).map(_.ticks.unrefined), Right(BigInt(3)))
    assertEquals(instrument.prices.fromTicks(PositiveWhole(3).toOption.get).coefficient, Rational(3, 2))

  test("component-based construction rejects contradictory roles, wrong grids, and empty payoff"):
    val identity = Identity(InstrumentId("invalid"), UnderlyingId("index:not-a-currency"))
    val roles    = new Roles(fixture.btc, fixture.btc, fixture.contract, fixture.usd)
    val listing  = new ListingRules(roles)(fixture.contractLots, fixture.usdPerBtcTicks)
    val payoff   = new ContractPayoff(roles)(
      Rate(roles.position.dimension.ref, roles.base.dimension.ref, Rational.one),
      Rate(roles.position.dimension.ref, roles.quote.dimension.ref, Rational.zero)
    )
    assertEquals(
      Instrument.create(Definition(identity, roles, listing, payoff)),
      Left(ContradictoryInstrument(identity.id, Contradiction.BaseEqualsQuote))
    )

    val validRoles   = new Roles(fixture.btc, fixture.usd, fixture.contract, fixture.usd)
    val wrongListing = new ListingRules(validRoles)(fixture.usdCents, fixture.usdPerBtcTicks)
    val empty        = new ContractPayoff(validRoles)(
      Rate(validRoles.position.dimension.ref, validRoles.base.dimension.ref, Rational.zero),
      Rate(validRoles.position.dimension.ref, validRoles.quote.dimension.ref, Rational.zero)
    )
    val wrongGrid = Instrument.create(Definition(identity, validRoles, wrongListing, empty))
    assert(wrongGrid.swap.exists(_.isInstanceOf[GridDimensionFailure]))

    val validListing = new ListingRules(validRoles)(fixture.contractLots, fixture.usdPerBtcTicks)
    assertEquals(
      Instrument.create(Definition(identity, validRoles, validListing, empty)),
      Left(EmptyContractPayoff(identity.id))
    )

    val foreign        = new EconomicsFixtures
    val foreignListing = new ListingRules(validRoles)(foreign.contractLots, foreign.usdPerBtcTicks)
    assert(
      Instrument
        .create(Definition(identity, validRoles, foreignListing, empty))
        .swap
        .exists(_.isInstanceOf[ForeignRegistry])
    )

    val otherRoles           = new Roles(fixture.btc, fixture.usd, fixture.contract, fixture.usd)
    val contradictoryListing = new ListingRules(otherRoles)(fixture.contractLots, fixture.usdPerBtcTicks)
    assertEquals(
      Instrument.create(Definition(identity, validRoles, contradictoryListing, empty)),
      Left(ContradictoryInstrument(identity.id, Contradiction.ListingRolesDiffer))
    )

  test("market states preserve exact anchor equations and own settle conversion"):
    val price        = fixture.price(instrument, 100)
    val quoteSettled = instrument.market.quoteSettled(price).toOption.get
    assertEquals(quoteSettled.baseToSettle.coefficient, Rational(100))
    assertEquals(quoteSettled.quoteToSettle.coefficient, Rational.one)
    assertEquals(
      quoteSettled
        .convertToSettle(fixture.usd)(Quantity(fixture.usd.dimension.ref, Rational(7)))
        .map(_.coefficient),
      Right(Rational(7))
    )

    val token     = instrument.market.conversion(fixture.token, Rational(2)).toOption.get
    val withToken = instrument.market.quoteSettled(price, Vector(token)).toOption.get
    assertEquals(
      withToken
        .convertToSettle(fixture.token)(Quantity(fixture.token.dimension.ref, Rational(3)))
        .map(_.coefficient),
      Right(Rational(6))
    )
    assertEquals(
      instrument.market.fromAnchors(price, Rational(99), Rational.one),
      Left(IncoherentMarketState(Rational(100), Rational(99), Rational.one))
    )

    val scalar    = instrument.market.fromQuoteAnchor(price, Rational.one).toOption.get
    val typedRate = Rate(
      instrument.roles.quote.dimension.ref,
      instrument.roles.settle.dimension.ref,
      Rational.one
    )
    val typed = instrument.market.fromQuoteRate(price, typedRate).toOption.get
    assertEquals(scalar.baseToSettle.coefficient, typed.baseToSettle.coefficient)

  test("linear, inverse, quanto, and spot-like valuation retain exact formulas"):
    val linearLots     = fixture.linear.lots(1000).toOption.get
    val linearPosition = fixture.linear.positionLots(Side.Buy, linearLots).toOption.get
    val linearEntry    = fixture.state(fixture.linear, 100)
    val linearExit     = fixture.state(fixture.linear, 110)
    assertEquals(
      fixture.linear.valuation.pricePnl(linearPosition, linearEntry, linearExit).map(_.coefficient),
      Right(Rational(10))
    )

    val inverseLots     = fixture.inverse.lots(1000).toOption.get
    val inversePosition = fixture.inverse.positionLots(Side.Buy, inverseLots).toOption.get
    val inverseEntry    = fixture.inverse.market.baseSettled(fixture.price(fixture.inverse, 100)).toOption.get
    val inverseExit     = fixture.inverse.market.baseSettled(fixture.price(fixture.inverse, 110)).toOption.get
    assertEquals(
      fixture.inverse.valuation.pricePnl(inversePosition, inverseEntry, inverseExit).map(_.coefficient),
      Right(Rational(1, 11))
    )

    val quantoPrice = fixture.price(fixture.quanto, 100)
    val quoteToEur  = Rate(
      fixture.quanto.roles.quote.dimension.ref,
      fixture.quanto.roles.settle.dimension.ref,
      Rational(9, 10)
    )
    val quantoState = fixture.quanto.market.fromQuoteRate(quantoPrice, quoteToEur).toOption.get
    assertEquals(fixture.quanto.valuation.settlePerPosition(quantoState).map(_.coefficient), Right(Rational(90)))

    val spotLots = fixture.spotLike.lots(100_000_000).toOption.get
    assertEquals(spotLots.quantity.coefficient, Rational.one)

end InstrumentEconomicsSuite
