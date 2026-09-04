package trading.scenario

import munit.FunSuite

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.reference.Asset

final class ScenarioValuationSuite extends FunSuite:
  private val fixture = new InstrumentFixtures

  private def scenario[I <: Instrument](
    instrument: I
  )(
    side: Side,
    totalCount: BigInt,
    slices: Vector[(BigInt, instrument.MarketState)]
  ): OrderScenario[instrument.roles.position.D, instrument.roles.base.D, instrument.roles.quote.D, instrument.MarketState] =
    val total   = fixture.lots(instrument, totalCount)
    val order   = Order.market(instrument)(side, total).toOption.get
    val matched = slices.map: (count, state) =>
      LiquiditySlice
        .create(instrument)(fixture.lots(instrument, count), state, LiquidityRole.Taker)
        .toOption
        .get
    val assumptions = ScenarioAssumptions
      .fromVector(order)(
        order.activation.evidence,
        order.execution.resolution,
        matched
      )
      .toOption
      .get
    OrderScenario.evaluate(instrument)(assumptions).toOption.get
  end scenario

  private def roundTrip[I <: Instrument](
    instrument: I
  )(
    entrySide: Side,
    entrySlices: Vector[(BigInt, instrument.MarketState)],
    exitSlices: Vector[(BigInt, instrument.MarketState)]
  ): RoundTripScenario[instrument.roles.position.D, instrument.roles.base.D, instrument.roles.quote.D, instrument.MarketState] =
    val total = entrySlices.map(_._1).sum
    require(exitSlices.map(_._1).sum == total)
    val exitSide = entrySide match
      case Side.Buy  => Side.Sell
      case Side.Sell => Side.Buy
    RoundTripScenario
      .create(instrument)(
        scenario(instrument)(entrySide, total, entrySlices),
        scenario(instrument)(exitSide, total, exitSlices)
      )
      .toOption
      .get
  end roundTrip
  private def assertOneSliceEquivalence[I <: Instrument](
    instrument: I
  )(
    entry: instrument.MarketState,
    exit: instrument.MarketState,
    expectedCoefficient: Rational
  ): Unit =
    val longTrip = roundTrip(instrument)(Side.Buy, Vector(BigInt(1000) -> entry), Vector(BigInt(1000) -> exit))
    val longCore = PricePnl.calculate(instrument)(longTrip.heldPosition, entry, exit).toOption.get
    val long     = ScenarioValuation.pricePnl(instrument)(longTrip).toOption.get

    assertEquals(long, longCore)
    assertEquals(long.quantity.coefficient, expectedCoefficient)
    assert(long.settlement.eq(instrument.roles.settle))
    val longAttributed = ScenarioValuation.attributedPricePnl(instrument)(longTrip).toOption.get
    assertEquals(
      longAttributed.settledContributions.map(_.attribution),
      Vector(
        RoundTripPriceAttribution(RoundTripLeg.Entry, 0),
        RoundTripPriceAttribution(RoundTripLeg.Exit, 0)
      )
    )
    assertEquals(
      longAttributed.settledContributions.map(_.positionChange.coordinate),
      Vector(BigInt(1000), BigInt(-1000))
    )

    val shortTrip = roundTrip(instrument)(Side.Sell, Vector(BigInt(1000) -> exit), Vector(BigInt(1000) -> entry))
    val shortCore = PricePnl.calculate(instrument)(shortTrip.heldPosition, exit, entry).toOption.get
    val short     = ScenarioValuation.pricePnl(instrument)(shortTrip).toOption.get

    assertEquals(short, shortCore)
    assertEquals(short.quantity.coefficient, expectedCoefficient)
    assert(short.settlement.eq(instrument.roles.settle))
    val shortAttributed = ScenarioValuation.attributedPricePnl(instrument)(shortTrip).toOption.get
    assertEquals(
      shortAttributed.settledContributions.map(_.attribution),
      Vector(
        RoundTripPriceAttribution(RoundTripLeg.Entry, 0),
        RoundTripPriceAttribution(RoundTripLeg.Exit, 0)
      )
    )
    assertEquals(
      shortAttributed.settledContributions.map(_.positionChange.coordinate),
      Vector(BigInt(-1000), BigInt(1000))
    )
  end assertOneSliceEquivalence

  test("one slice per leg exactly equals core exit-minus-entry for linear, inverse, and quanto payoffs"):
    assertOneSliceEquivalence(fixture.linear)(
      fixture.quoteState(fixture.linear, Rational(100)),
      fixture.quoteState(fixture.linear, Rational(110)),
      Rational(10)
    )

    assertOneSliceEquivalence(fixture.inverse)(
      MarketState
        .baseSettled(fixture.inverse)(fixture.price(fixture.inverse, Rational(100)))
        .toOption
        .get,
      MarketState
        .baseSettled(fixture.inverse)(fixture.price(fixture.inverse, Rational(110)))
        .toOption
        .get,
      Rational(1, 11)
    )

    assertOneSliceEquivalence(fixture.quanto)(
      MarketState
        .fromQuoteAnchor(fixture.quanto)(fixture.price(fixture.quanto, Rational(100)), Rational(7, 11))
        .toOption
        .get,
      MarketState
        .fromQuoteAnchor(fixture.quanto)(fixture.price(fixture.quanto, Rational(110)), Rational(7, 11))
        .toOption
        .get,
      Rational(70, 11)
    )

  test("multi-slice linear normalization weights each exact lot count for long and short round trips"):
    val entry: Vector[(BigInt, fixture.linear.MarketState)] = Vector(
      BigInt(4) -> fixture.quoteState(fixture.linear, Rational(100)),
      BigInt(6) -> fixture.quoteState(fixture.linear, Rational(102))
    )
    val exit: Vector[(BigInt, fixture.linear.MarketState)] = Vector(
      BigInt(3) -> fixture.quoteState(fixture.linear, Rational(110)),
      BigInt(7) -> fixture.quoteState(fixture.linear, Rational(112))
    )
    val longTrip  = roundTrip(fixture.linear)(Side.Buy, entry, exit)
    val shortTrip = roundTrip(fixture.linear)(Side.Sell, exit, entry)
    val long      = ScenarioValuation.pricePnl(fixture.linear)(longTrip).toOption.get
    val short     = ScenarioValuation.pricePnl(fixture.linear)(shortTrip).toOption.get

    assertEquals(long.quantity.coefficient, Rational(51, 500))
    assertEquals(short.quantity.coefficient, long.quantity.coefficient)
    assertNotEquals(long.quantity.coefficient, Rational(1, 10))

    val expectedAttributions = Vector(
      RoundTripPriceAttribution(RoundTripLeg.Entry, 0),
      RoundTripPriceAttribution(RoundTripLeg.Entry, 1),
      RoundTripPriceAttribution(RoundTripLeg.Exit, 0),
      RoundTripPriceAttribution(RoundTripLeg.Exit, 1)
    )
    val attributed = ScenarioValuation.attributedPricePnl(fixture.linear)(longTrip).toOption.get
    assertEquals(
      attributed.settledContributions.map(_.attribution),
      expectedAttributions
    )
    assertEquals(
      attributed.settledContributions.map(_.positionChange.coordinate),
      Vector(BigInt(4), BigInt(6), BigInt(-3), BigInt(-7))
    )
    assertEquals(attributed.pricePnl, long)

    val shortAttributed = ScenarioValuation.attributedPricePnl(fixture.linear)(shortTrip).toOption.get
    assertEquals(shortAttributed.settledContributions.map(_.attribution), expectedAttributions)
    assertEquals(
      shortAttributed.settledContributions.map(_.positionChange.coordinate),
      Vector(BigInt(-3), BigInt(-7), BigInt(4), BigInt(6))
    )
    assertEquals(shortAttributed.pricePnl, short)

  test("multi-slice inverse normalization retains exact reciprocal weighting"):
    def state(price: Rational): fixture.inverse.MarketState =
      MarketState.baseSettled(fixture.inverse)(fixture.price(fixture.inverse, price)).toOption.get

    val trip = roundTrip(fixture.inverse)(
      Side.Buy,
      Vector(BigInt(400) -> state(Rational(100)), BigInt(600) -> state(Rational(120))),
      Vector(BigInt(300) -> state(Rational(110)), BigInt(700) -> state(Rational(130)))
    )
    val result = ScenarioValuation.pricePnl(fixture.inverse)(trip).toOption.get

    assertEquals(result.quantity.coefficient, Rational(127, 1430))

  test("multi-slice third-asset quanto normalization preserves off-grid exactness without quantization"):
    def state(price: Rational): fixture.quanto.MarketState =
      MarketState
        .fromQuoteAnchor(fixture.quanto)(fixture.price(fixture.quanto, price), Rational(7, 11))
        .toOption
        .get

    val trip = roundTrip(fixture.quanto)(
      Side.Buy,
      Vector(BigInt(400) -> state(Rational(100)), BigInt(600) -> state(Rational(120))),
      Vector(BigInt(300) -> state(Rational(110)), BigInt(700) -> state(Rational(130)))
    )
    val result = ScenarioValuation.pricePnl(fixture.quanto)(trip).toOption.get

    assertEquals(result.settlement, fixture.quanto.roles.settle)
    assertEquals(result.quantity.coefficient, Rational(84, 11))

  test("order intent derives slice positions through typed identity checks"):
    val instrument = fixture.linear
    val order      = Order.market(instrument)(Side.Sell, fixture.lots(instrument, 10)).toOption.get
    val localLots  = fixture.lots(instrument, 4)
    assertEquals(
      order.intent.positionChangeFor(instrument)(localLots).map(_.coordinate),
      Right(BigInt(-4))
    )

    val foreign       = fixture.foreignIdentity
    val foreignLots   = fixture.lots(foreign, 4)
    val foreignIntent = Order
      .market(foreign)(Side.Buy, foreignLots)
      .toOption
      .get
      .intent
      .asInstanceOf[OrderIntent[instrument.roles.position.D]]
    assertEquals(
      foreignIntent.positionChangeFor(instrument)(localLots),
      Left(
        OrderViolation.InstrumentMismatch(
          OrderComponent.Intent,
          instrument.identity.id,
          foreign.identity.id
        )
      )
    )
    assertEquals(
      order.intent.positionChangeFor(instrument)(foreignLots.asInstanceOf[instrument.Lots]),
      Left(
        OrderViolation.InstrumentMismatch(
          OrderComponent.Lots,
          instrument.identity.id,
          foreign.identity.id
        )
      )
    )

  test("foreign round trips fail at the typed normalization identity gate"):
    val foreign     = fixture.foreignIdentity
    val foreignTrip = roundTrip(foreign)(
      Side.Buy,
      Vector(BigInt(10) -> fixture.quoteState(foreign, Rational(100))),
      Vector(BigInt(10) -> fixture.quoteState(foreign, Rational(110)))
    ).asInstanceOf[
      RoundTripScenario[
        fixture.linear.roles.position.D,
        fixture.linear.roles.base.D,
        fixture.linear.roles.quote.D,
        fixture.linear.MarketState
      ]
    ]

    assertEquals(
      ScenarioValuation.pricePnl(fixture.linear)(foreignTrip),
      Left(
        ScenarioValuationError.InstrumentMismatch(
          fixture.linear.identity.id,
          foreign.identity.id
        )
      )
    )

  test("delegated failures retain the real round-trip leg and slice location"):
    val instrument     = fixture.linear
    val foreignLineage = new InstrumentFixtures
    val foreignMarket  = foreignLineage
      .quoteState(foreignLineage.linear, Rational(112))
      .asInstanceOf[instrument.MarketState]
    val referenceCause = Asset.reconcile(foreignMarket.base, instrument.roles.base).swap.toOption.get
    val trip           = roundTrip(instrument)(
      Side.Buy,
      Vector(BigInt(10) -> fixture.quoteState(instrument, Rational(100))),
      Vector(
        BigInt(4) -> fixture.quoteState(instrument, Rational(110)),
        BigInt(6) -> foreignMarket
      )
    )

    assertEquals(
      ScenarioValuation.pricePnl(instrument)(trip),
      Left(
        ScenarioValuationError.SliceValue(
          RoundTripLeg.Exit,
          1,
          ValuationReferenceDataMismatch(
            "market.base.reference",
            referenceCause
          )
        )
      )
    )
end ScenarioValuationSuite
