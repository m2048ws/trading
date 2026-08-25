package trading.economics

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole

class FeePnlSizingSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  test("percentage fees use account signs and preserve exact quantization conservation"):
    val charge = instrument
      .percentageFee(fixture.usd)(
        fixture.usdCents,
        FeeKind("taker"),
        Quantity(fixture.usd.dimension.asDimensionRef, Rational(21, 20)),
        FeeRate(Rational(1, 10)),
        QuantizationPolicy.TowardZero
      )
      .toOption
      .get
    val rebate = instrument
      .percentageFee(fixture.usd)(
        fixture.usdCents,
        FeeKind("maker"),
        Quantity(fixture.usd.dimension.asDimensionRef, Rational(21, 20)),
        FeeRate(Rational(-1, 10)),
        QuantizationPolicy.TowardZero
      )
      .toOption
      .get

    assertEquals(charge.coordinate, BigInt(-10))
    assertEquals(charge.amount.coefficient, Rational(-1, 10))
    assertEquals(charge.residual.coefficient, Rational(-1, 200))
    assertEquals(charge.amount.coefficient + charge.residual.coefficient, charge.unrounded.coefficient)
    assertEquals(rebate.coordinate, BigInt(10))
    assertEquals(rebate.unrounded.coefficient, Rational(21, 200))

  test("fees retain an explicit third asset and grid"):
    val fee = instrument
      .quantizeFee(fixture.token)(
        fixture.tokenMillis,
        FeeKind("token-flat"),
        Quantity(fixture.token.dimension.asDimensionRef, Rational(-7, 3)),
        QuantizationPolicy.Floor
      )
      .toOption
      .get

    assertEquals(fee.asset.id, fixture.token.id)
    assertEquals(fee.gridKey, fixture.tokenMillis.key)
    assertEquals(fee.coordinate, BigInt(-2334))
    assertEquals(fee.amount.coefficient + fee.residual.coefficient, fee.unrounded.coefficient)

  test("contextual schedules inspect mechanics and mixed maker/taker slices"):
    val lots     = instrument.lots(10).toOption.get
    val order    = instrument.limitOrder(Side.Buy, lots, fixture.price(instrument, 100)).toOption.get
    val state    = instrument.marketStateForQuote(fixture.price(instrument, 100)).toOption.get
    val maker    = instrument.liquiditySlice(instrument.lots(4).toOption.get, state, LiquidityRole.Maker)
    val taker    = instrument.liquiditySlice(instrument.lots(6).toOption.get, state, LiquidityRole.Taker)
    val scenario = instrument.orderScenario(order, Vector(maker, taker)).toOption.get
    val schedule = new instrument.FeeSchedule:
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        scenario.slices.zipWithIndex.foldLeft[Either[EconomicsError, Vector[instrument.FeeLine]]](Right(Vector.empty)):
          case (result, (slice, index)) =>
            result.flatMap: accumulated =>
              val rate =
                if slice.role == LiquidityRole.Maker then FeeRate(Rational(-1, 100)) else FeeRate(Rational(2, 100))
              val basis = Quantity(fixture.usd.dimension.asDimensionRef, Rational(instrument.lotCount(slice.lots)))
              for
                fee <- instrument.percentageFee(fixture.usd)(
                         fixture.usdCents,
                         FeeKind(s"${scenario.order.visibility.kind}-$index"),
                         basis,
                         rate,
                         QuantizationPolicy.TowardZero
                       )
                line <- instrument.feeLine(scenario, index, fee)
              yield accumulated :+ line

    val lines = schedule.assess(scenario).toOption.get
    assertEquals(lines.size, 2)
    assert(lines.head.fee.amount.coefficient.signum > 0)
    assert(lines(1).fee.amount.coefficient.signum < 0)
    assert(lines.forall(_.fee.kind.value.startsWith("Displayed")))

  test("captured account tiers, minimum charges, flat fees, and composition stay outside Instrument"):
    val accountTier = 2
    val scenario    = completeMarket(Side.Buy, instrument.lots(10).toOption.get, 100)
    val tiered      = new instrument.FeeSchedule:
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val rate  = if accountTier >= 2 then FeeRate(Rational(1, 1000)) else FeeRate(Rational(2, 1000))
        val basis = Quantity(fixture.usd.dimension.asDimensionRef, Rational(1))
        for
          calculated <- instrument.percentageFee(fixture.usd)(
                          fixture.usdCents,
                          FeeKind("tiered"),
                          basis,
                          rate,
                          QuantizationPolicy.TowardZero
                        )
          minimum <- instrument.applyMinimumCharge(calculated.asset)(
                       calculated.unrounded,
                       Quantity(calculated.asset.dimension.asDimensionRef, Rational(1, 100))
                     )
          fee <- instrument.quantizeFee(calculated.asset)(
                   fixture.usdCents,
                   FeeKind("tiered-minimum"),
                   minimum,
                   QuantizationPolicy.TowardZero
                 )
          line <- instrument.feeLine(scenario, 0, fee)
        yield Vector(line)
      end assess
    val flat = new instrument.FeeSchedule:
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        for
          fee <- instrument.quantizeFee(fixture.usd)(
                   fixture.usdCents,
                   FeeKind("flat-component"),
                   Quantity(fixture.usd.dimension.asDimensionRef, Rational(-1, 50)),
                   QuantizationPolicy.TowardZero
                 )
          line <- instrument.feeLine(scenario, 0, fee)
        yield Vector(line)

    val lines = instrument.combineFeeSchedules(Vector(tiered, flat)).assess(scenario).toOption.get
    assertEquals(lines.map(_.fee.kind), Vector(FeeKind("tiered-minimum"), FeeKind("flat-component")))
    assertEquals(lines.map(_.fee.amount.coefficient), Vector(Rational(-1, 100), Rational(-1, 50)))

  test("fee lines reject invalid indices and PnL rejects attribution to another scenario"):
    val first  = completeMarket(Side.Buy, instrument.lots(10).toOption.get, 100)
    val second = completeMarket(Side.Buy, instrument.lots(10).toOption.get, 101)
    val fee    = instrument
      .quantizeFee(fixture.usd)(
        fixture.usdCents,
        FeeKind("flat"),
        Quantity(fixture.usd.dimension.asDimensionRef, Rational(-1)),
        QuantizationPolicy.TowardZero
      )
      .toOption
      .get

    assert(instrument.feeLine(first, 1, fee).isLeft)
    val foreignLine = instrument.feeLine(second, 0, fee).toOption.get
    val schedule    = new instrument.FeeSchedule:
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        Right(Vector(foreignLine))
    val exit      = completeMarket(Side.Sell, instrument.lots(10).toOption.get, 110)
    val roundTrip = instrument.roundTrip(first, exit).toOption.get
    assert(instrument.calculatePnl(roundTrip, schedule).isLeft)

  test("multi-slice PnL retains price and fee components with per-line conversion states"):
    val lots            = instrument.lots(1000).toOption.get
    val entryConversion = tokenToUsd(Rational(2))
    val exitConversion  = tokenToUsd(Rational(3))
    val entry           = completeMarket(Side.Buy, lots, 100, Vector(entryConversion))
    val exit            = completeMarket(Side.Sell, lots, 110, Vector(exitConversion))
    val roundTrip       = instrument.roundTrip(entry, exit).toOption.get
    val schedule        = flatTokenSchedule(Rational(-1))
    val pnl             = instrument.calculatePnl(roundTrip, schedule).toOption.get

    assertEquals(pnl.pricePnl.coefficient, Rational(10))
    assertEquals(pnl.convertedFeeLines.map(_.settleContribution.coefficient), Vector(Rational(-2), Rational(-3)))
    assertEquals(pnl.feePnl.coefficient, Rational(-5))
    assertEquals(pnl.netPnl.coefficient, Rational(5))
    assertEquals(pnl.pricePnl.coefficient + pnl.feePnl.coefficient, pnl.netPnl.coefficient)
    assertEquals(pnl.convertedFeeLines.map(_.leg), Vector(ScenarioLeg.Entry, ScenarioLeg.Exit))

  test("price PnL sums every matched slice without an average-price representation"):
    val lots       = instrument.lots(1000).toOption.get
    val entryOrder = instrument.marketOrder(Side.Buy, lots).toOption.get
    val first      = instrument.liquiditySlice(
      instrument.lots(500).toOption.get,
      instrument.marketStateForQuote(fixture.price(instrument, 99)).toOption.get,
      LiquidityRole.Taker
    )
    val second = instrument.liquiditySlice(
      instrument.lots(500).toOption.get,
      instrument.marketStateForQuote(fixture.price(instrument, 101)).toOption.get,
      LiquidityRole.Taker
    )
    val entry     = instrument.orderScenario(entryOrder, Vector(first, second)).toOption.get
    val exit      = completeMarket(Side.Sell, lots, 110)
    val roundTrip = instrument.roundTrip(entry, exit).toOption.get
    val pnl       = instrument.calculatePnl(roundTrip, instrument.noFees).toOption.get

    assertEquals(pnl.pricePnl.coefficient, Rational(10))

  test("missing fee conversion is typed with leg, slice, and asset"):
    val lots      = instrument.lots(1000).toOption.get
    val entry     = completeMarket(Side.Buy, lots, 100)
    val exit      = completeMarket(Side.Sell, lots, 110)
    val roundTrip = instrument.roundTrip(entry, exit).toOption.get

    instrument.calculatePnl(roundTrip, flatTokenSchedule(Rational(-1))) match
      case Left(MissingConversion(asset, Some(ScenarioLeg.Entry), Some(0))) => assertEquals(asset, fixture.token.id)
      case other => fail(s"expected attributed missing conversion, obtained $other")

  test("PnL is deterministic and downside risk is exact and nonnegative"):
    val lots      = instrument.lots(1000).toOption.get
    val entry     = completeMarket(Side.Buy, lots, 100)
    val exit      = completeMarket(Side.Sell, lots, 90)
    val roundTrip = instrument.roundTrip(entry, exit).toOption.get
    val first     = instrument.calculatePnl(roundTrip, instrument.noFees).toOption.get
    val second    = instrument.calculatePnl(roundTrip, instrument.noFees).toOption.get

    assertEquals(first.netPnl.coefficient, second.netPnl.coefficient)
    assertEquals(instrument.downsideRisk(first).coefficient, Rational(10))
    val profitable = instrument.calculatePnl(
      instrument.roundTrip(entry, completeMarket(Side.Sell, lots, 110)).toOption.get,
      instrument.noFees
    ).toOption.get
    assertEquals(instrument.downsideRisk(profitable).coefficient, Rational.zero)

  test("sizing evaluates every positive candidate and selects the greatest exact affordable count"):
    val budget = settleAmount(Rational(3, 100))
    val result = instrument.sizePosition(budget, PositiveWhole(5).toOption.get, instrument.noFees): candidate =>
      losingRoundTrip(candidate, 100, 90)

    assertEquals(result.map(_.map(instrument.lotCount)), Right(Some(BigInt(3))))

  test("sizing rejects the first round trip whose held position does not match its candidate"):
    val oneLot          = instrument.lots(1).toOption.get
    val oneLotRoundTrip = losingRoundTrip(oneLot, 100, 90).toOption.get
    var evaluated       = Vector.empty[BigInt]
    val result          = instrument.sizePosition(settleAmount(Rational(10)), PositiveWhole(3).toOption.get,
      instrument.noFees): candidate =>
      val candidateCount = instrument.lotCount(candidate)
      evaluated :+= candidateCount
      if candidateCount == BigInt(1) then losingRoundTrip(candidate, 100, 90)
      else Right(oneLotRoundTrip)

    assertEquals(result, Left(SizingScenarioMismatch(BigInt(2), BigInt(1))))
    assertEquals(evaluated, Vector(BigInt(1), BigInt(2)))

  test("sizing supports no-result, capped, exact boundary, and profitable cases"):
    val tinyBudget = settleAmount(Rational(1, 1000))
    val none       = instrument.sizePosition(tinyBudget, PositiveWhole(3).toOption.get, instrument.noFees): candidate =>
      losingRoundTrip(candidate, 100, 90)
    assertEquals(none, Right(None))

    val exactBudget = settleAmount(Rational(1, 50))
    val exact = instrument.sizePosition(exactBudget, PositiveWhole(2).toOption.get, instrument.noFees): candidate =>
      losingRoundTrip(candidate, 100, 90)
    assertEquals(exact.map(_.map(instrument.lotCount)), Right(Some(BigInt(2))))

    val zeroBudget = settleAmount(Rational.zero)
    val profitable = instrument.sizePosition(zeroBudget, PositiveWhole(4).toOption.get, instrument.noFees): candidate =>
      losingRoundTrip(candidate, 100, 110)
    assertEquals(profitable.map(_.map(instrument.lotCount)), Right(Some(BigInt(4))))

  test("sizing does not assume monotonicity and propagates the first candidate failure"):
    val budget    = settleAmount(Rational(1, 200))
    val nonlinear = instrument.sizePosition(budget, PositiveWhole(3).toOption.get, instrument.noFees): candidate =>
      if instrument.lotCount(candidate) == BigInt(2) then losingRoundTrip(candidate, 100, 99)
      else losingRoundTrip(candidate, 100, 90)
    assertEquals(nonlinear.map(_.map(instrument.lotCount)), Right(Some(BigInt(2))))

    val failed = instrument.sizePosition(budget, PositiveWhole(4).toOption.get, instrument.noFees): candidate =>
      if instrument.lotCount(candidate) == BigInt(2) then Left(FeeScheduleFailure("candidate-two"))
      else if instrument.lotCount(candidate) == BigInt(3) then Left(FeeScheduleFailure("candidate-three"))
      else losingRoundTrip(candidate, 100, 99)
    assertEquals(failed, Left(FeeScheduleFailure("candidate-two")))

  test("sizing uses both legs and ordinary fee quantization"):
    val conversion = tokenToUsd(Rational.one)
    val budget     = settleAmount(Rational(3, 4))
    val schedule   = flatTokenSchedule(Rational(-1, 2))
    val result     = instrument.sizePosition(budget, PositiveWhole(3).toOption.get, schedule): candidate =>
      val entry = completeMarket(Side.Buy, candidate, 100, Vector(conversion))
      val exit  = completeMarket(Side.Sell, candidate, 100, Vector(conversion))
      instrument.roundTrip(entry, exit)

    assertEquals(result, Right(None))

  test("sizing observes adjacent fee-grid steps, missing conversions, and invalid exits"):
    val steppedSchedule = new instrument.FeeSchedule:
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val count = instrument.lotCount(scenario.order.lots)
        val exact = Quantity(fixture.usd.dimension.asDimensionRef, Rational(-3 * count, 1000))
        for
          fee <- instrument.quantizeFee(fixture.usd)(
                   fixture.usdCents,
                   FeeKind("stepped"),
                   exact,
                   QuantizationPolicy.TowardZero
                 )
          line <- instrument.feeLine(scenario, 0, fee)
        yield Vector(line)
    val oneCent = settleAmount(Rational(1, 100))
    val stepped = instrument.sizePosition(oneCent, PositiveWhole(5).toOption.get, steppedSchedule): candidate =>
      instrument.roundTrip(
        completeMarket(Side.Buy, candidate, 100),
        completeMarket(Side.Sell, candidate, 100)
      )
    assertEquals(stepped.map(_.map(instrument.lotCount)), Right(Some(BigInt(3))))

    val missing = instrument.sizePosition(settleAmount(Rational(100)), PositiveWhole(2).toOption.get,
      flatTokenSchedule(Rational(-1))): candidate =>
      instrument.roundTrip(
        completeMarket(Side.Buy, candidate, 100),
        completeMarket(Side.Sell, candidate, 100)
      )
    assert(missing.left.exists(_.isInstanceOf[MissingConversion]))

    val invalidExit = instrument.sizePosition(settleAmount(Rational(100)), PositiveWhole(2).toOption.get,
      instrument.noFees): candidate =>
      val smaller = instrument.lots(instrument.lotCount(candidate) + 1).toOption.get
      instrument.roundTrip(
        completeMarket(Side.Buy, candidate, 100),
        completeMarket(Side.Sell, smaller, 100)
      )
    assert(invalidExit.left.exists(_.isInstanceOf[InvalidRoundTrip]))

  private def tokenToUsd(coefficient: Rational): SettlementConversion =
    SettlementConversion
      .positive(fixture.token, fixture.usd)(
        Rate(fixture.token.dimension.asDimensionRef, fixture.usd.dimension.asDimensionRef, coefficient)
      )
      .toOption
      .get

  private def settleAmount(coefficient: Rational): Quantity[instrument.settle.D] =
    Quantity(instrument.settle.dimension.asDimensionRef, coefficient)

  private def flatTokenSchedule(unrounded: Rational): instrument.FeeSchedule = new instrument.FeeSchedule:
    def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
      scenario.slices.zipWithIndex.foldLeft[Either[EconomicsError, Vector[instrument.FeeLine]]](Right(Vector.empty)):
        case (result, (_, index)) =>
          result.flatMap: accumulated =>
            for
              fee <- instrument.quantizeFee(fixture.token)(
                       fixture.tokenMillis,
                       FeeKind("flat-token"),
                       Quantity(fixture.token.dimension.asDimensionRef, unrounded),
                       QuantizationPolicy.TowardZero
                     )
              line <- instrument.feeLine(scenario, index, fee)
            yield accumulated :+ line

  private def completeMarket(
    side: Side,
    lots: instrument.Lots,
    dollars: BigInt,
    conversions: Vector[SettlementConversion] = Vector.empty
  ): instrument.OrderScenario =
    val order  = instrument.marketOrder(side, lots).toOption.get
    val market = instrument.marketStateForQuote(fixture.price(instrument, dollars), conversions).toOption.get
    val slice  = instrument.liquiditySlice(lots, market, LiquidityRole.Taker)
    instrument.orderScenario(order, Vector(slice)).toOption.get

  private def losingRoundTrip(
    lots: instrument.Lots,
    entryDollars: BigInt,
    exitDollars: BigInt
  ): Either[EconomicsError, instrument.RoundTripScenario] =
    instrument.roundTrip(
      completeMarket(Side.Buy, lots, entryDollars),
      completeMarket(Side.Sell, lots, exitDollars)
    )

end FeePnlSizingSuite
