package trading.economics

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole

class RuntimeInstrumentMismatchSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear
  private val expected   = instrument.identity.id
  private val foreign    = InstrumentId("foreign-instrument")
  private val lots       = instrument.lots(10).toOption.get
  private val price      = fixture.price(instrument, 100)
  private val market     = fixture.state(instrument, 100)

  test("named identity helper accepts matches and reports the first named mismatch"):
    assertEquals(
      InstrumentIdentityChecks.check("test", expected, "left" -> expected, "right" -> expected),
      Right(())
    )
    assertEquals(
      InstrumentIdentityChecks.check("test", expected, "left" -> expected, "right" -> foreign),
      Left(InstrumentMismatch("test.right", expected, foreign))
    )

  test("market and order boundaries reject ordinary foreign runtime identities"):
    val foreignPrice = price.copy(instrumentId = foreign)
    assertEquals(
      instrument.market.quoteSettled(foreignPrice),
      Left(InstrumentMismatch("market.price", expected, foreign))
    )

    val foreignLots = lots.copy(instrumentId = foreign)
    assertEquals(
      instrument.orders.market(Side.Buy, foreignLots),
      Left(InstrumentMismatch("order.intent", expected, foreign))
    )

  test("scenario and round-trip boundaries reject ordinary foreign runtime identities"):
    val order        = instrument.orders.market(Side.Buy, lots).toOption.get
    val foreignSlice = InstrumentLiquiditySlice(
      foreign,
      lots,
      market,
      LiquidityRole.Taker
    )
    val assumptions = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.directPricing,
      Vector(foreignSlice)
    )
    assertEquals(
      instrument.scenarios.order(order, assumptions),
      Left(InstrumentMismatch("scenario.slices[0]", expected, foreign))
    )

    val entry = fixture.scenario(instrument)(Side.Buy, lots, market)
    val exit  = fixture.scenario(instrument)(Side.Sell, lots, fixture.state(instrument, 90))
    assertEquals(
      instrument.scenarios.roundTrip(entry, exit.copy(instrumentId = foreign)),
      Left(InstrumentMismatch("roundTrip.exit", expected, foreign))
    )

  test("fee and valuation boundaries reject ordinary foreign runtime identities"):
    val scenario     = fixture.scenario(instrument)(Side.Buy, lots, market)
    val denomination = instrument.fees
      .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val fee = denomination.quantize(
      FeeKind("foreign"),
      Quantity(fixture.usd.dimension.asDimensionRef, Rational(-1, 100))
    )
    assertEquals(
      instrument.fees.line(scenario, 0, fee.copy(instrumentId = foreign)),
      Left(InstrumentMismatch("fee.line.fee", expected, foreign))
    )

    val roundTrip = fixture.roundTrip(instrument)(lots, 100, 90)
    assertEquals(
      instrument.valuation.pnl(roundTrip.copy(instrumentId = foreign), instrument.fees.none),
      Left(InstrumentMismatch("valuation.pnl.roundTrip", expected, foreign))
    )

  test("sizing rejects a callback result for another runtime instrument"):
    val result = instrument.sizing.maxLots(
      Quantity(instrument.roles.settle.dimension.asDimensionRef, Rational(100)),
      PositiveWhole(1).toOption.get,
      instrument.fees.none
    ): candidate =>
      Right(fixture.roundTrip(instrument)(candidate, 100, 90).copy(instrumentId = foreign))

    assertEquals(result, Left(InstrumentMismatch("sizing.maxLots.scenario", expected, foreign)))

end RuntimeInstrumentMismatchSuite
