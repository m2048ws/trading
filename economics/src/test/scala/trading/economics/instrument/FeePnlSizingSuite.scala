package trading.economics.instrument

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole

class FeePnlSizingSuite extends FunSuite:
  private val fixture         = new EconomicsFixtures
  private val instrument      = fixture.linear
  private val usdDenomination = instrument.fees
    .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.TowardZero)
    .toOption
    .get

  test("validated denominations are reusable and preserve charge, rebate, and residual signs"):
    val basis  = Quantity(fixture.usd.dimension.ref, Rational(10))
    val charge = usdDenomination.percentage(FeeKind("taker"), basis, FeeRate(Rational(1, 1000))).toOption.get
    val rebate = usdDenomination.percentage(FeeKind("maker"), basis, FeeRate(Rational(-1, 1000))).toOption.get
    assertEquals(charge.amount.coefficient, Rational(-1, 100))
    assertEquals(rebate.amount.coefficient, Rational(1, 100))
    assertEquals(charge.amount.coefficient + charge.residual.coefficient, charge.unrounded.coefficient)
    assertEquals(rebate.amount.coefficient + rebate.residual.coefficient, rebate.unrounded.coefficient)
    assertEquals(charge.denomination, usdDenomination)

  test("denomination rejects foreign or dimension-mismatched grids before calculating fees"):
    assert(instrument.fees.denomination(fixture.usd)(fixture.tokenMillis, QuantizationPolicy.TowardZero).isLeft)

    val foreign = new EconomicsFixtures
    assert(instrument.fees.denomination(foreign.usd)(foreign.usdCents, QuantizationPolicy.TowardZero).isLeft)

  test("third-asset fee conversion preserves attribution and exact net PnL"):
    val lots       = instrument.lots(1000).toOption.get
    val tokenToUsd = instrument.market.conversion(fixture.token, Rational(2)).toOption.get
    val entryState = instrument.market
      .quoteSettled(fixture.price(instrument, 100), Vector(tokenToUsd))
      .toOption
      .get
    val exitPrice         = fixture.price(instrument, 90)
    val exitState         = instrument.market.quoteSettled(exitPrice, Vector(tokenToUsd)).toOption.get
    val entry             = fixture.scenario(instrument)(Side.Buy, lots, entryState)
    val exit              = fixture.scenario(instrument)(Side.Sell, lots, exitState)
    val roundTrip         = instrument.scenarios.roundTrip(entry, exit).toOption.get
    val tokenDenomination = instrument.fees
      .denomination(fixture.token)(fixture.tokenMillis, QuantizationPolicy.TowardZero)
      .toOption
      .get

    val schedule = new instrument.FeeSchedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val fee = tokenDenomination.quantize(
          FeeKind("token-flat"),
          Quantity(fixture.token.dimension.ref, Rational(-1, 1000))
        )
        instrument.fees.line(scenario, 0, fee).map(Vector(_))

    val pnl = instrument.valuation.pnl(roundTrip, schedule).toOption.get
    assertEquals(pnl.pricePnl.coefficient, Rational(-10))
    assertEquals(pnl.convertedFeeLines.size, 2)
    assertEquals(pnl.feePnl.coefficient, Rational(-1, 250))
    assertEquals(pnl.netPnl.coefficient, Rational(-2501, 250))
    assertEquals(pnl.convertedFeeLines.map(_.sourceSliceIndex), Vector(0, 0))

  test("missing fee conversion is typed and annotated with leg and slice"):
    val lots              = instrument.lots(1000).toOption.get
    val roundTrip         = fixture.roundTrip(instrument)(lots, 100, 90)
    val tokenDenomination = instrument.fees
      .denomination(fixture.token)(fixture.tokenMillis, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val schedule = new instrument.FeeSchedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val fee = tokenDenomination.quantize(
          FeeKind("missing-token"),
          Quantity(fixture.token.dimension.ref, Rational(-1, 1000))
        )
        instrument.fees.line(scenario, 0, fee).map(Vector(_))
    assertEquals(
      instrument.valuation.pnl(roundTrip, schedule),
      Left(MissingConversion(fixture.token.id, Some(ScenarioLeg.Entry), Some(0)))
    )

  test("schedule composition handles zero and many components and rejects foreign attribution"):
    val lots     = instrument.lots(1000).toOption.get
    val scenario = fixture.scenario(instrument)(Side.Buy, lots, fixture.state(instrument, 100))
    assertEquals(instrument.fees.none.assess(scenario), Right(Vector.empty))

    def component(kind: String, amount: Rational): instrument.FeeSchedule = new instrument.FeeSchedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(value: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val fee = usdDenomination.quantize(FeeKind(kind), Quantity(fixture.usd.dimension.ref, amount))
        instrument.fees.line(value, 0, fee).map(Vector(_))

    val combined =
      instrument.fees
        .combine(Vector(component("one", Rational(-1, 100)), component("two", Rational(-1, 50))))
        .toOption
        .get
    assertEquals(
      combined.assess(scenario).map(_.map(_.fee.amount.coefficient)),
      Right(Vector(Rational(-1, 100), Rational(-1, 50)))
    )

    val otherScenario = fixture.scenario(instrument)(Side.Buy, lots, fixture.state(instrument, 100))
    val foreignLine   = component("foreign", Rational(-1, 100)).assess(otherScenario).toOption.get
    val invalid       = new instrument.FeeSchedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(
        value: instrument.OrderScenario
      ): Either[EconomicsError, Vector[instrument.FeeLine]] = Right(foreignLine)
    val roundTrip = instrument.scenarios.roundTrip(scenario,
      fixture.scenario(instrument)(Side.Sell, lots, fixture.state(instrument, 90))).toOption.get
    assertEquals(
      instrument.valuation.pnl(roundTrip, invalid),
      Left(FeeScheduleFailure(FeeScheduleFailureReason.ForeignScenarioLine))
    )

  test("minimum charges preserve account-perspective sign"):
    val contribution = Quantity(fixture.usd.dimension.ref, Rational(-1, 1000))
    val minimum      = Quantity(fixture.usd.dimension.ref, Rational(1, 100))
    assertEquals(
      usdDenomination.minimumCharge(contribution, minimum).map(_.coefficient),
      Right(Rational(-1, 100))
    )
    assert(
      usdDenomination
        .minimumCharge(contribution, Quantity(fixture.usd.dimension.ref, Rational(-1)))
        .isLeft
    )

  test("contextual schedules can retain tiered policy without instrument metadata"):
    val lots     = instrument.lots(1000).toOption.get
    val scenario = fixture.scenario(instrument)(Side.Buy, lots, fixture.state(instrument, 100))
    val tiered   = new instrument.FeeSchedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(value: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val rate =
          if value.order.intent.lots.count.unrefined >= 1000 then FeeRate(Rational(1, 500))
          else FeeRate(Rational(1, 1000))
        val basis = Quantity(fixture.usd.dimension.ref, Rational(10))
        for
          fee  <- usdDenomination.percentage(FeeKind("tiered"), basis, rate)
          line <- instrument.fees.line(value, 0, fee)
        yield Vector(line)
    assertEquals(
      tiered.assess(scenario).map(_.head.fee.amount.coefficient),
      Right(Rational(-1, 50))
    )

  test("sizing exhaustively preserves candidate order, non-monotone selection, failure propagation, and fees"):
    val cap     = PositiveWhole(4).toOption.get
    val budget  = Quantity(instrument.roles.settle.dimension.ref, Rational(3, 100))
    val visited = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    val sized   = instrument.sizing.maxLots(budget, cap, instrument.fees.none): candidate =>
      visited += candidate.count.unrefined
      Right(fixture.roundTrip(instrument)(candidate, 100, 90))
    assertEquals(visited.toVector, Vector(1, 2, 3, 4).map(BigInt(_)))
    assertEquals(sized.map(_.map(_.count.unrefined)), Right(Some(BigInt(3))))

    val nonlinear = instrument.sizing.maxLots(budget, cap, instrument.fees.none): candidate =>
      if candidate.count.unrefined == 2 then Right(fixture.roundTrip(instrument)(candidate, 100, 90))
      else Right(fixture.roundTrip(instrument)(candidate, 100, 101))
    assertEquals(nonlinear.map(_.map(_.count.unrefined)), Right(Some(BigInt(4))))

    val failure = InvalidRiskBudget(Rational(-1))
    val failed  = instrument.sizing.maxLots(budget, cap, instrument.fees.none): candidate =>
      if candidate.count.unrefined == 2 then Left(failure)
      else Right(fixture.roundTrip(instrument)(candidate, 100, 90))
    assertEquals(failed, Left(failure))

    val flatFee = new instrument.FeeSchedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(scenario: instrument.OrderScenario): Either[EconomicsError, Vector[instrument.FeeLine]] =
        val fee = usdDenomination.quantize(
          FeeKind("flat"),
          Quantity(fixture.usd.dimension.ref, Rational(-1, 100))
        )
        instrument.fees.line(scenario, 0, fee).map(Vector(_))
    val withFees = instrument.sizing.maxLots(budget, cap, flatFee): candidate =>
      Right(fixture.roundTrip(instrument)(candidate, 100, 90))
    assertEquals(withFees.map(_.map(_.count.unrefined)), Right(Some(BigInt(1))))

end FeePnlSizingSuite
