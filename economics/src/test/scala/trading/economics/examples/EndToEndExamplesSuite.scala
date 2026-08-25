package trading.economics.examples

import munit.FunSuite

import trading.economics.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole

/** Public end-to-end usage of conditional scenario economics, without execution or account lifecycle state. */
class EndToEndExamplesSuite extends FunSuite:
  test("calculate and size one exact fee-inclusive scenario"):
    val fixture    = new EconomicsFixtures
    val instrument = fixture.linear
    val lots       = instrument.lots(1000).toOption.get
    val entry      = completeScenario(instrument)(Side.Buy, lots, "100")
    val exit       = completeScenario(instrument)(Side.Sell, lots, "90")
    val roundTrip  = instrument.scenarios.roundTrip(entry, exit).toOption.get

    // FeeRate uses policy sign (positive charge); Fee uses the account sign (negative charge). Quantization is
    // explicit.
    val schedule = new instrument.FeeSchedule:
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val exactBasis = Quantity(
          fixture.usd.dimension.asDimensionRef,
          Rational(instrument.lotCount(scenario.order.lots), 10)
        )
        for
          fee <- instrument.fees.percentage(fixture.usd)(
                   fixture.usdCents,
                   FeeKind("example-taker"),
                   exactBasis,
                   FeeRate(Rational(1, 1000)),
                   QuantizationPolicy.TowardZero
                 )
          line <- instrument.fees.line(scenario, 0, fee)
        yield Vector(line)

    val pnl = instrument.valuation.pnl(roundTrip, schedule).toOption.get
    assertEquals(pnl.pricePnl.coefficient, Rational(-10))
    assertEquals(pnl.feePnl.coefficient, Rational(-1, 5))
    assertEquals(pnl.netPnl.coefficient, Rational(-51, 5))

    // Sizing exhaustively reuses complete scenarios, both fee legs, explicit conversions, and the instrument lot grid.
    val sized = instrument.sizing.maxLots(
      Quantity(instrument.settle.dimension.asDimensionRef, Rational(11)),
      PositiveWhole(4).toOption.get,
      schedule
    ): candidate =>
      instrument.scenarios.roundTrip(
        completeScenario(instrument)(Side.Buy, candidate, "100"),
        completeScenario(instrument)(Side.Sell, candidate, "90")
      )

    assertEquals(sized.map(_.map(instrument.lotCount)), Right(Some(BigInt(4))))

  private def completeScenario(
    instrument: Instrument
  )(
    side: Side,
    lots: instrument.Lots,
    rawPrice: String
  ): instrument.OrderScenario =
    val coefficient = Rational.parse(rawPrice).toOption.get
    val price       = instrument.prices.exact(coefficient).toOption.get
    val order       = instrument.orders.market(side, lots).toOption.get
    val state       = instrument.market.quoteSettled(price).toOption.get
    val slice       = instrument.scenarios.slice(lots, state, LiquidityRole.Taker)
    instrument.scenarios.order(order, Vector(slice)).toOption.get

end EndToEndExamplesSuite
