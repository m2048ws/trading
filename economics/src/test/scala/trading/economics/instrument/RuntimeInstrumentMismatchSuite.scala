package trading.economics.instrument

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole

class RuntimeInstrumentMismatchSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear
  private val expected   = instrument.identity.id
  private val foreign    = InstrumentId.from("foreign-instrument").toOption.get
  private val lots       = instrument.lots(10).toOption.get
  private val price      = fixture.price(instrument, 100)
  private val market     = fixture.state(instrument, 100)

  test("named identity helper accepts matches and reports the first named mismatch"):
    assertEquals(
      IdentityChecks.check("test", expected, "left" -> expected, "right" -> expected),
      Right(())
    )
    assertEquals(
      IdentityChecks.check("test", expected, "left" -> expected, "right" -> foreign),
      Left(Mismatch("test.right", expected, foreign))
    )

  test("market and order boundaries reject ordinary foreign runtime identities"):
    val foreignPrice = price.copy(instrumentId = foreign)
    assertEquals(
      instrument.market.quoteSettled(foreignPrice),
      Left(Mismatch("market.price", expected, foreign))
    )

    val foreignLots = lots.copy(instrumentId = foreign)
    assertEquals(
      instrument.orders.market(Side.Buy, foreignLots),
      Left(Mismatch("order.intent", expected, foreign))
    )

  test("scenario and round-trip boundaries reject ordinary foreign runtime identities"):
    val order        = instrument.orders.market(Side.Buy, lots).toOption.get
    val foreignSlice = LiquiditySlice(
      foreign,
      lots,
      market,
      LiquidityRole.Taker
    )
    val assumptions = instrument.scenarios.assumptionsOne(order)(
      order.activation.evidence,
      order.execution.resolution,
      foreignSlice
    )
    assertEquals(
      instrument.scenarios.order(order, assumptions),
      Left(Mismatch("scenario.slices[0]", expected, foreign))
    )

    val entry = fixture.scenario(instrument)(Side.Buy, lots, market)
    val exit  = fixture.scenario(instrument)(Side.Sell, lots, fixture.state(instrument, 90))
    assertEquals(
      instrument.scenarios.roundTrip(entry, exit.copy(instrumentId = foreign)),
      Left(Mismatch("roundTrip.exit", expected, foreign))
    )

  test("fee and valuation boundaries reject ordinary foreign runtime identities"):
    val scenario     = fixture.scenario(instrument)(Side.Buy, lots, market)
    val denomination = instrument.fees
      .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val fee = denomination.quantize(
      FeeKind("foreign"),
      Quantity(fixture.usd.dimension.ref, Rational(-1, 100))
    )
    assertEquals(
      instrument.fees.line(scenario, 0, fee.copy(instrumentId = foreign)),
      Left(Mismatch("fee.line.fee", expected, foreign))
    )

    val roundTrip = fixture.roundTrip(instrument)(lots, 100, 90)
    assertEquals(
      instrument.valuation.pnl(roundTrip.copy(instrumentId = foreign), instrument.fees.none),
      Left(Mismatch("valuation.pnl.roundTrip", expected, foreign))
    )

  test("sizing rejects a callback result for another runtime instrument"):
    val result = instrument.sizing.maxLots(
      Quantity(instrument.roles.settle.dimension.ref, Rational(100)),
      PositiveWhole(1).toOption.get,
      instrument.fees.none
    ): candidate =>
      Right(fixture.roundTrip(instrument)(candidate, 100, 90).copy(instrumentId = foreign))

    assertEquals(result, Left(Mismatch("sizing.maxLots.scenario", expected, foreign)))

end RuntimeInstrumentMismatchSuite
