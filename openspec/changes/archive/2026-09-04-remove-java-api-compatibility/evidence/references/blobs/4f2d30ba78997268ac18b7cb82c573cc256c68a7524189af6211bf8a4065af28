package trading

import munit.FunSuite

import trading.economics.instrument.*
import trading.fee.*
import trading.order.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.NonNegative
import trading.quantity.refinement.PositiveWhole
import trading.risk.*
import trading.scenario.*
import trading.support.DownstreamFixtures

class FeePolicyIntegrationSuite extends FunSuite:
  private val fixture    = new DownstreamFixtures
  private val instrument = fixture.linear

  private type Scenario = OrderScenario[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.MarketState
  ]
  private type RoundTrip = RoundTripScenario[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.MarketState
  ]
  private type Policy[+E] = FeePolicy[
    E,
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.roles.settle.D
  ]

  private def scenario(
    side: Side,
    lots: instrument.Lots,
    state: instrument.MarketState,
    role: LiquidityRole = LiquidityRole.Taker
  ): Scenario =
    val order       = Order.market(instrument)(side, lots).toOption.get
    val slice       = LiquiditySlice.create(instrument)(lots, state, role).toOption.get
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice
    )
    OrderScenario.evaluate(instrument)(assumptions).toOption.get

  private def roundTrip(
    lots: instrument.Lots,
    entry: instrument.MarketState,
    exit: instrument.MarketState
  ): RoundTrip =
    RoundTripScenario
      .create(instrument)(
        scenario(Side.Buy, lots, entry),
        scenario(Side.Sell, lots, exit)
      )
      .toOption
      .get

  test("scenario construction retains non-empty slices and deterministic accumulated diagnostics"):
    val lots        = Lots.fromCount(instrument)(1000).toOption.get
    val order       = Order.limit(instrument)(Side.Buy, lots, fixture.price(instrument, Rational(100))).toOption.get
    val badPrice    = fixture.state(instrument, Rational(101))
    val slice       = LiquiditySlice.create(instrument)(lots, badPrice, LiquidityRole.Maker).toOption.get
    val matched     = MatchedSlices.one(slice)
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.pricing.resolution,
      slice
    )
    val errors = OrderScenario.evaluate(instrument)(assumptions).swap.toOption.get
    assertEquals(
      errors.violations,
      Vector(ScenarioViolation.SliceWorseThanLimit(0))
    )
    assertEquals(
      OrderScenario.evaluateFirst(instrument)(assumptions),
      Left(ScenarioViolation.SliceWorseThanLimit(0))
    )
    assertEquals(matched.head, slice)
    assertEquals(matched.tail, Vector.empty)
    assertEquals(matched.toVector, Vector(slice))
    assertEquals(MatchedSlices.fromVector(Vector(slice)), Right(matched))

    val empty = ScenarioAssumptions.fromVector(order)(
      order.activation.evidence,
      order.execution.pricing.resolution,
      Vector.empty
    )
    assertEquals(empty, Left(ScenarioViolation.EmptySlices))
    assertEquals(MatchedSlices.fromVector(Vector.empty), Left(ScenarioViolation.EmptySlices))

  test("scenario diagnostics accumulate independent slice failures in stable order"):
    val total      = Lots.fromCount(instrument)(10).toOption.get
    val firstLots  = Lots.fromCount(instrument)(4).toOption.get
    val secondLots = Lots.fromCount(instrument)(6).toOption.get
    val badMarket  = fixture.state(instrument, Rational(101))
    val first      = LiquiditySlice.create(instrument)(firstLots, badMarket, LiquidityRole.Taker).toOption.get
    val second     = LiquiditySlice.create(instrument)(secondLots, badMarket, LiquidityRole.Taker).toOption.get
    val order      = Order
      .limit(instrument)(
        Side.Buy,
        total,
        fixture.price(instrument, Rational(100)),
        liquidityConstraint = LiquidityConstraint.MakerOnly
      )
      .toOption
      .get
    val assumptions = ScenarioAssumptions.many(order)(
      order.activation.evidence,
      order.execution.pricing.resolution,
      first,
      second
    )
    val expected = Vector(
      ScenarioViolation.MakerOnlySliceNotMaker(0),
      ScenarioViolation.MakerOnlySliceNotMaker(1),
      ScenarioViolation.SliceWorseThanLimit(0),
      ScenarioViolation.SliceWorseThanLimit(1)
    )
    assertEquals(OrderScenario.evaluate(instrument)(assumptions).left.map(_.violations), Left(expected))
    assertEquals(
      OrderScenario.evaluateFirst(instrument)(assumptions),
      Left(ScenarioViolation.MakerOnlySliceNotMaker(0))
    )

  test("round trips require exact flat closure"):
    val entryLots = Lots.fromCount(instrument)(1000).toOption.get
    val exitLots  = Lots.fromCount(instrument)(999).toOption.get
    val entry     = scenario(Side.Buy, entryLots, fixture.state(instrument, Rational(100)))
    val exit      = scenario(Side.Sell, exitLots, fixture.state(instrument, Rational(110)))
    assertEquals(
      RoundTripScenario.create(instrument)(entry, exit),
      Left(RoundTripViolation.PositionNotFlat(BigInt(1000), BigInt(-999)))
    )

  test("fee policy owns percentage and minimum logic while core owns exact fee construction"):
    val denomination = FeeDenomination
      .create(instrument)(fixture.usd, fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val kind  = FeeKind.from("taker").toOption.get
    val basis = NonNegative(Quantity(fixture.usd.dimension.ref, Rational(10))).toOption.get
    val fee   = Fee
      .create(instrument)(
        denomination,
        kind,
        FeeCalculation.percentage(basis, FeeRate(Rational(1, 1000)))
      )
      .toOption
      .get
    val rebate = Fee
      .create(instrument)(
        denomination,
        FeeKind.from("maker").toOption.get,
        FeeCalculation.percentage(basis, FeeRate(Rational(-1, 1000)))
      )
      .toOption
      .get
    assertEquals(fee.amount.coefficient, Rational(-1, 100))
    assertEquals(rebate.amount.coefficient, Rational(1, 100))
    assertEquals(fee.amount + fee.residual, fee.unrounded)
    assertEquals(rebate.amount + rebate.residual, rebate.unrounded)
    assertEquals(
      FeeCalculation
        .minimumCharge(
          Quantity(fixture.usd.dimension.ref, Rational(-1, 1000)),
          NonNegative(Quantity(fixture.usd.dimension.ref, Rational(1, 100))).toOption.get
        )
        .coefficient,
      Rational(-1, 100)
    )

  test("canonical composition converts each fee at its selected state and retains contribution order"):
    val lots             = Lots.fromCount(instrument)(1000).toOption.get
    val tokenConversion1 = SettlementConversion.exact(instrument)(fixture.token)(Rational(2)).toOption.get
    val tokenConversion2 = SettlementConversion.exact(instrument)(fixture.token)(Rational(3)).toOption.get
    val entry            = MarketState
      .quoteSettled(instrument)(fixture.price(instrument, Rational(100)), Vector(tokenConversion1))
      .toOption
      .get
    val exit = MarketState
      .quoteSettled(instrument)(fixture.price(instrument, Rational(110)), Vector(tokenConversion2))
      .toOption
      .get
    val trip         = roundTrip(lots, entry, exit)
    val denomination = FeeDenomination
      .create(instrument)(fixture.token, fixture.tokenMillis, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val fee = Fee
      .create(instrument)(
        denomination,
        FeeKind.from("token").toOption.get,
        Quantity(fixture.token.dimension.ref, Rational(-1, 1000))
      )
      .toOption
      .get
    val feeStrategy = new Policy[Nothing]:
      val instrumentId: InstrumentId                                                     = instrument.identity.id
      def evaluate(value: Scenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
        Right(Vector(FeeDirective(fee, SliceIndex.zero)))

    val pnl = FeeInclusivePnl
      .evaluate(instrument)(trip, RoundTripFeePolicies.same(feeStrategy))
      .toOption
      .get
    assertEquals(pnl.pricePnl.quantity.coefficient, Rational(10))
    assertEquals(
      pnl.settledFeeContributions.map(_.quantity.coefficient),
      Vector(Rational(-1, 500), Rational(-3, 1000))
    )
    assertEquals(pnl.feePnl.coefficient, Rational(-1, 200))
    assertEquals(pnl.netPnl.coefficient, Rational(1999, 200))

  test("no-fee PnL and exact downside risk compose without core policy services"):
    val lots = Lots.fromCount(instrument)(1000).toOption.get
    val trip = roundTrip(
      lots,
      fixture.state(instrument, Rational(100)),
      fixture.state(instrument, Rational(90))
    )
    val pnl = FeeInclusivePnl
      .evaluate(instrument)(trip, RoundTripFeePolicies.same(FeePolicy.noFees(instrument)))
      .toOption
      .get
    assertEquals(pnl.netPnl.coefficient, Rational(-10))
    assertEquals(Risk.downside(instrument)(pnl.pnl).map(_.unrefined.coefficient), Right(Rational(10)))

  test("missing fee conversions retain both legs and source-slice attribution"):
    val lots = Lots.fromCount(instrument)(1000).toOption.get
    val trip = roundTrip(
      lots,
      fixture.state(instrument, Rational(100)),
      fixture.state(instrument, Rational(90))
    )
    val denomination = FeeDenomination
      .create(instrument)(fixture.token, fixture.tokenMillis, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val fee = Fee
      .create(instrument)(
        denomination,
        FeeKind.from("missing-token").toOption.get,
        Quantity(fixture.token.dimension.ref, Rational(-1, 1000))
      )
      .toOption
      .get
    val feeStrategy = new Policy[Nothing]:
      val instrumentId: InstrumentId                                                     = instrument.identity.id
      def evaluate(value: Scenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
        Right(Vector(FeeDirective(fee, SliceIndex.zero)))

    assertEquals(
      FeeInclusivePnl.evaluate(instrument)(trip, RoundTripFeePolicies.same(feeStrategy)),
      Left(
        FeeInclusivePnlErrors.of(
          FeeInclusiveConversionFailure(
            RoundTripLeg.Entry,
            0,
            SliceIndex.zero,
            ContributionConversionFailure(MissingConversion(fixture.token.id))
          ),
          FeeInclusiveConversionFailure(
            RoundTripLeg.Exit,
            0,
            SliceIndex.zero,
            ContributionConversionFailure(MissingConversion(fixture.token.id))
          )
        )
      )
    )

  test("a fixed adverse-exit table earns a monotone model before primary sizing"):
    val cap          = PositiveWhole(4).toOption.get
    val budget       = NonNegative(Quantity(instrument.roles.settle.dimension.ref, Rational(3, 100))).toOption.get
    val observations = 1.to(4).toVector.map: count =>
      val lots = Lots.fromCount(instrument)(count).toOption.get
      val pnl  = FeeInclusivePnl
        .evaluate(instrument)(
          roundTrip(
            lots,
            fixture.state(instrument, Rational(100)),
            fixture.state(instrument, Rational(90))
          ),
          RoundTripFeePolicies.same(FeePolicy.noFees(instrument))
        )
        .toOption
        .get
      lots -> pnl.pnl
    val model    = MonotoneLotRisk.fromCompleteTable(instrument)(cap, observations).toOption.get
    val selected = MaxAffordableLots.select(model)(budget)

    selected match
      case MaxAffordableLots.Selected(best, AffordableUpperBoundary.NextUnaffordable(next), _) =>
        assertEquals(best.lots.count.unrefined, BigInt(3))
        assertEquals(next.lots.count.unrefined, BigInt(4))
      case other => fail(s"expected checked-table interior selection, received $other")

  test("arbitrary scenario evaluation uses the explicit exhaustive fallback with located failures"):
    val cap      = PositiveWhole(4).toOption.get
    val budget   = NonNegative(Quantity(instrument.roles.settle.dimension.ref, Rational(3, 100))).toOption.get
    val visited  = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    val selected = ExhaustiveLotSizing.select(instrument)(budget, cap): lots =>
      visited += lots.count.unrefined
      FeeInclusivePnl.evaluate(instrument)(
        roundTrip(
          lots,
          fixture.state(instrument, Rational(100)),
          fixture.state(instrument, Rational(90))
        ),
        RoundTripFeePolicies.same(FeePolicy.noFees(instrument))
      ).map(_.pnl)
    assertEquals(visited.toVector, Vector(1, 2, 3, 4).map(BigInt(_)))
    assertEquals(
      selected.map:
        case ExhaustiveLotDecision.Selected(best, _)  => best.lots.count.unrefined
        case ExhaustiveLotDecision.NoAffordable(_, _) => BigInt(0),
      Right(BigInt(3))
    )

    val nonlinear = ExhaustiveLotSizing.select(instrument)(budget, cap): lots =>
      val exit = if lots.count.unrefined == 2 then Rational(90) else Rational(101)
      FeeInclusivePnl.evaluate(instrument)(
        roundTrip(
          lots,
          fixture.state(instrument, Rational(100)),
          fixture.state(instrument, exit)
        ),
        RoundTripFeePolicies.same(FeePolicy.noFees(instrument))
      ).map(_.pnl)
    assertEquals(
      nonlinear.map:
        case ExhaustiveLotDecision.Selected(best, _)  => best.lots.count.unrefined
        case ExhaustiveLotDecision.NoAffordable(_, _) => BigInt(0),
      Right(BigInt(4))
    )

    val failure = RoundTripViolation.PositionNotFlat(BigInt(2), BigInt(-1))
    val failed  = ExhaustiveLotSizing.select(instrument)(budget, cap): lots =>
      if lots.count.unrefined == 2 then Left(failure)
      else
        Right(
          FeeInclusivePnl
            .evaluate(instrument)(
              roundTrip(
                lots,
                fixture.state(instrument, Rational(100)),
                fixture.state(instrument, Rational(90))
              ),
              RoundTripFeePolicies.same(FeePolicy.noFees(instrument))
            )
            .map(_.pnl)
            .toOption
            .get
        )
    assertEquals(
      failed.left.map(value => (value.coordinate.unrefined, value.cause)),
      Left((BigInt(2), ExhaustiveLotEvaluationCause.CallerEvaluation(failure)))
    )

  test("a fixed flat-fee table earns a monotone model without moving fee policy into risk"):
    val cap          = PositiveWhole(4).toOption.get
    val budget       = NonNegative(Quantity(instrument.roles.settle.dimension.ref, Rational(3, 100))).toOption.get
    val denomination = FeeDenomination
      .create(instrument)(fixture.usd, fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val flatFee = Fee
      .create(instrument)(
        denomination,
        FeeKind.from("flat").toOption.get,
        Quantity(fixture.usd.dimension.ref, Rational(-1, 100))
      )
      .toOption
      .get
    val feeStrategy = new Policy[Nothing]:
      val instrumentId: InstrumentId                                                     = instrument.identity.id
      def evaluate(value: Scenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
        Right(Vector(FeeDirective(flatFee, SliceIndex.zero)))
    val observations = 1.to(4).toVector.map: count =>
      val lots = Lots.fromCount(instrument)(count).toOption.get
      val pnl  = FeeInclusivePnl
        .evaluate(instrument)(
          roundTrip(
            lots,
            fixture.state(instrument, Rational(100)),
            fixture.state(instrument, Rational(90))
          ),
          RoundTripFeePolicies.same(feeStrategy)
        )
        .toOption
        .get
      lots -> pnl.pnl
    val model    = MonotoneLotRisk.fromCompleteTable(instrument)(cap, observations).toOption.get
    val withFees = MaxAffordableLots.select(model)(budget)
    assertEquals(
      withFees match
        case MaxAffordableLots.Selected(best, _, _) => Some(best.lots.count.unrefined)
        case MaxAffordableLots.NoAffordable(_, _)   => None,
      Some(BigInt(1))
    )

  test("policy composition preserves zero and ordered directives while rejecting foreign components"):
    val lots      = Lots.fromCount(instrument)(1000).toOption.get
    val evaluated = scenario(Side.Buy, lots, fixture.state(instrument, Rational(100)))
    assertEquals(FeePolicy.noFees(instrument).evaluate(evaluated), Right(Vector.empty))

    val denomination = FeeDenomination
      .create(instrument)(fixture.usd, fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    def component(name: String, amount: Rational): Policy[Nothing] = new Policy[Nothing]:
      val instrumentId: InstrumentId                                                     = instrument.identity.id
      def evaluate(value: Scenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
        val fee = Fee
          .create(instrument)(
            denomination,
            FeeKind.from(name).toOption.get,
            Quantity(fixture.usd.dimension.ref, amount)
          )
          .toOption
          .get
        Right(Vector(FeeDirective(fee, SliceIndex.zero)))

    val combined = FeePolicy
      .combine(instrument)(Vector(component("one", Rational(-1, 100)), component("two", Rational(1, 100))))
      .toOption
      .get
    assertEquals(
      combined.evaluate(evaluated).map(_.map(_.fee.amount.coefficient)),
      Right(Vector(Rational(-1, 100), Rational(1, 100)))
    )

    val foreign = new Policy[Nothing]:
      val instrumentId: InstrumentId                                                     = fixture.foreign.identity.id
      def evaluate(value: Scenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
        Right(Vector.empty)
    assertEquals(
      FeePolicy
        .combine(instrument)(Vector(component("local", Rational.zero), foreign))
        .left
        .map(_.toVector),
      Left(Vector(ForeignPolicyInstrument(1, instrument.identity.id, fixture.foreign.identity.id)))
    )
end FeePolicyIntegrationSuite
