package trading

import munit.FunSuite

import trading.economics.instrument.*
import trading.fee.policy.*
import trading.order.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole
import trading.risk.*
import trading.scenario.*
import trading.support.DownstreamFixtures

class DownstreamEconomicsSuite extends FunSuite:
  private val fixture    = new DownstreamFixtures
  private val instrument = fixture.linear
  private val orders     = Orders(instrument)
  private val scenarios  = Scenarios(instrument)
  private val policy     = FeePolicy(instrument)

  private def scenario(
    side: Side,
    lots: instrument.Lots,
    state: instrument.MarketState,
    role: LiquidityRole = LiquidityRole.Taker
  ): policy.Scenario =
    val order       = orders.market(side, lots).toOption.get
    val slice       = scenarios.slice(lots, state, role).toOption.get
    val assumptions = scenarios.assumptionsOne(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice
    )
    scenarios.order(order, assumptions).toOption.get

  private def roundTrip(
    lots: instrument.Lots,
    entry: instrument.MarketState,
    exit: instrument.MarketState
  ): policy.RoundTrip =
    scenarios
      .roundTrip(
        scenario(Side.Buy, lots, entry),
        scenario(Side.Sell, lots, exit)
      )
      .toOption
      .get

  test("order side is downstream and derives an exact signed position change"):
    val lots = Lots.fromCount(instrument)(1000).toOption.get
    val buy  = orders.market(Side.Buy, lots).toOption.get
    val sell = orders.market(Side.Sell, lots).toOption.get
    assertEquals(buy.intent.positionChange.coordinate, BigInt(1000))
    assertEquals(sell.intent.positionChange.coordinate, BigInt(-1000))
    assertEquals(buy.instrumentId, instrument.identity.id)

  test("final order construction rejects a copied position change that contradicts side and lots"):
    val lots             = Lots.fromCount(instrument)(2).toOption.get
    val canonical        = orders.intent(Side.Buy, lots)
    val execution        = orders.marketExecution(NonRestingTimeInForce.ImmediateOrCancel)
    val forgedCoordinate = canonical.copy(
      positionChange = PositionLots.fromCoordinate(instrument)(-999)
    )

    assertEquals(
      orders.create(forgedCoordinate, orders.immediate, execution),
      Left(InvalidOrder(OrderFailureReason.PositionChangeMismatch(2, -999)))
    )

    assertEquals(
      orders.create(canonical, orders.immediate, execution).map(_.intent.positionChange.coordinate),
      Right(BigInt(2))
    )

  test("downstream order alternatives retain instruction-shape validation"):
    val lots     = Lots.fromCount(instrument)(10).toOption.get
    val limit    = fixture.price(instrument, Rational(100))
    val fixed    = orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, limit)
    val trailing = orders
      .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 5)
      .toOption
      .get
    assertEquals(fixed.triggerPrice.ticks.unrefined, BigInt(200))
    assertEquals(trailing.offsetTicks.unrefined, BigInt(5))
    assertEquals(
      orders.trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 0),
      Left(InvalidTrailingOffset(0))
    )

    val oversized          = orders.iceberg(Lots.fromCount(instrument)(11).toOption.get)
    val oversizedExecution = orders.pricedExecution(
      orders.limitPricing(limit),
      TimeInForce.Day,
      LiquidityConstraint.Unrestricted,
      oversized
    )
    assertEquals(
      orders.create(orders.intent(Side.Buy, lots), orders.immediate, oversizedExecution),
      Left(InvalidOrder(OrderFailureReason.IcebergExceedsOrder(11, 10)))
    )

    val nonResting = orders.pricedExecution(
      orders.limitPricing(limit),
      TimeInForce.ImmediateOrCancel,
      LiquidityConstraint.Unrestricted,
      orders.iceberg(Lots.fromCount(instrument)(2).toOption.get)
    )
    assertEquals(
      orders.create(orders.intent(Side.Buy, lots), orders.immediate, nonResting),
      Left(InvalidOrder(OrderFailureReason.NonRestingIceberg))
    )
    assert(NonRestingTimeInForce.from(TimeInForce.Day).isLeft)

  test("downstream activation evidence and pricing resolutions retain their semantics"):
    val lots        = Lots.fromCount(instrument)(10).toOption.get
    val limit       = fixture.price(instrument, Rational(100))
    val state       = fixture.state(instrument, Rational(100))
    val taker       = scenarios.slice(lots, state, LiquidityRole.Taker).toOption.get
    val fixed       = orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, limit)
    val stop        = orders.stopMarket(Side.Buy, lots, fixed).toOption.get
    val evidence    = orders.fixedEvidence(fixed)(limit).toOption.get
    val assumptions = scenarios.assumptionsOne(stop)(evidence, stop.execution.resolution, taker)
    assert(scenarios.order(stop, assumptions).isRight)
    assertEquals(
      orders.fixedEvidence(fixed)(fixture.price(instrument, Rational(99))),
      Left(ActivationViolation.FixedTriggerUnsatisfied)
    )

    val changedFixed =
      orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, fixture.price(instrument, Rational(101)))
    assertEquals(changedFixed.verify(evidence), Left(ActivationViolation.FixedEvidenceMismatch))

    val peg        = orders.peggedPricing(PriceReference.Mark, 2)
    val reference  = fixture.price(instrument, Rational(99))
    val resolution = orders.pegResolution(peg)(reference, limit).toOption.get
    assertEquals(peg.resolve(resolution), Right(EffectivePricing.Limited(limit)))
    assertEquals(
      orders.peggedPricing(PriceReference.Mark, 3).resolve(resolution),
      Left(PricingViolation.PegResolutionMismatch)
    )

  test("scenario construction retains non-empty slices and deterministic accumulated diagnostics"):
    val lots        = Lots.fromCount(instrument)(1000).toOption.get
    val order       = orders.limit(Side.Buy, lots, fixture.price(instrument, Rational(100))).toOption.get
    val badPrice    = fixture.state(instrument, Rational(101))
    val slice       = scenarios.slice(lots, badPrice, LiquidityRole.Maker).toOption.get
    val assumptions = scenarios.assumptionsOne(order)(
      order.activation.evidence,
      order.execution.pricing.resolution,
      slice
    )
    val errors = scenarios.diagnose(order, assumptions).swap.toOption.get
    assertEquals(
      errors.violations,
      Vector(ScenarioViolation.Slice(0, ScenarioFailureReason.SliceWorseThanLimit))
    )
    assertEquals(scenarios.order(order, assumptions),
      Left(InvalidScenario(ScenarioFailureReason.SliceWorseThanLimit, Some(0))))

    val empty = scenarios.assumptionsFromVector(order)(
      order.activation.evidence,
      order.execution.pricing.resolution,
      Vector.empty
    )
    assertEquals(empty, Left(InvalidScenarioDiagnostics(ScenarioViolation.EmptySlices, Vector.empty)))

  test("scenario diagnostics accumulate independent slice failures in stable order"):
    val total      = Lots.fromCount(instrument)(10).toOption.get
    val firstLots  = Lots.fromCount(instrument)(4).toOption.get
    val secondLots = Lots.fromCount(instrument)(6).toOption.get
    val badMarket  = fixture.state(instrument, Rational(101))
    val first      = scenarios.slice(firstLots, badMarket, LiquidityRole.Taker).toOption.get
    val second     = scenarios.slice(secondLots, badMarket, LiquidityRole.Taker).toOption.get
    val order      = orders
      .limit(
        Side.Buy,
        total,
        fixture.price(instrument, Rational(100)),
        liquidityConstraint = LiquidityConstraint.MakerOnly
      )
      .toOption
      .get
    val assumptions = scenarios.assumptionsMany(order)(
      order.activation.evidence,
      order.execution.pricing.resolution,
      first,
      second
    )
    val expected = Vector(
      ScenarioViolation.Slice(0, ScenarioFailureReason.MakerOnlySliceNotMaker),
      ScenarioViolation.Slice(0, ScenarioFailureReason.SliceWorseThanLimit),
      ScenarioViolation.Slice(1, ScenarioFailureReason.MakerOnlySliceNotMaker),
      ScenarioViolation.Slice(1, ScenarioFailureReason.SliceWorseThanLimit)
    )
    assertEquals(scenarios.diagnose(order, assumptions).left.map(_.violations), Left(expected))
    assertEquals(
      scenarios.order(order, assumptions),
      Left(InvalidScenario(ScenarioFailureReason.MakerOnlySliceNotMaker, Some(0)))
    )

  test("round trips require exact flat closure"):
    val entryLots = Lots.fromCount(instrument)(1000).toOption.get
    val exitLots  = Lots.fromCount(instrument)(999).toOption.get
    val entry     = scenario(Side.Buy, entryLots, fixture.state(instrument, Rational(100)))
    val exit      = scenario(Side.Sell, exitLots, fixture.state(instrument, Rational(110)))
    assertEquals(
      scenarios.roundTrip(entry, exit),
      Left(InvalidRoundTrip(BigInt(1000), BigInt(-999)))
    )

  test("fee policy owns percentage and minimum logic while core owns exact fee construction"):
    val denomination = policy
      .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val kind  = FeeKind.from("taker").toOption.get
    val basis = Quantity(fixture.usd.dimension.ref, Rational(10))
    val fee   = policy.percentage(denomination, kind, basis, FeeRate(Rational(1, 1000))).toOption.get
    assertEquals(fee.amount.coefficient, Rational(-1, 100))
    assertEquals(fee.amount + fee.residual, fee.unrounded)
    assertEquals(
      policy
        .minimumCharge(
          Quantity(fixture.usd.dimension.ref, Rational(-1, 1000)),
          Quantity(fixture.usd.dimension.ref, Rational(1, 100)),
          fixture.usd.id
        )
        .map(_.coefficient),
      Right(Rational(-1, 100))
    )

  test("downstream orchestration converts each fee at its selected state and retains contribution order"):
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
    val denomination = policy
      .denomination(fixture.token)(fixture.tokenMillis, QuantizationPolicy.TowardZero)
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
    val schedule = new policy.Schedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(value: policy.Scenario): Either[FeePolicyError, Vector[FeeLine[? <: Dim, policy.Market]]] =
        policy.line(value, 0, fee).map(Vector(_))

    val pnl = policy.pnl(trip, schedule).toOption.get
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
    val pnl  = policy.pnl(trip, policy.none).toOption.get
    val risk = Risk.create(instrument)(policy).toOption.get
    assertEquals(pnl.netPnl.coefficient, Rational(-10))
    assertEquals(risk.downsideRisk(pnl).map(_.unrefined.coefficient), Right(Rational(10)))

  test("risk sizing selects the greatest exact discrete candidate and includes fee-inclusive PnL"):
    val risk     = Risk.create(instrument)(policy).toOption.get
    val cap      = PositiveWhole(4).toOption.get
    val budget   = Quantity(instrument.roles.settle.dimension.ref, Rational(3, 100))
    val selected = risk.maxLots(budget, cap, policy.none): candidate =>
      Right(
        roundTrip(
          candidate,
          fixture.state(instrument, Rational(100)),
          fixture.state(instrument, Rational(90))
        )
      )
    assertEquals(selected.map(_.map(_.count.unrefined)), Right(Some(BigInt(3))))

  test("policy composition preserves zero and ordered many schedules"):
    val lots      = Lots.fromCount(instrument)(1000).toOption.get
    val evaluated = scenario(Side.Buy, lots, fixture.state(instrument, Rational(100)))
    assertEquals(policy.none.assess(evaluated), Right(Vector.empty))

    val denomination = policy
      .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    def component(name: String, amount: Rational): policy.Schedule = new policy.Schedule:
      val instrumentId: InstrumentId = instrument.identity.id
      def assess(value: policy.Scenario): Either[FeePolicyError, Vector[FeeLine[? <: Dim, policy.Market]]] =
        for
          fee <- Fee
                   .create(instrument)(
                     denomination,
                     FeeKind.from(name).toOption.get,
                     Quantity(fixture.usd.dimension.ref, amount)
                   )
                   .left
                   .map(FeeValueFailure(_))
          line <- policy.line(value, 0, fee)
        yield Vector(line)

    val combined = policy
      .combine(Vector(component("one", Rational(-1, 100)), component("two", Rational(1, 100))))
      .toOption
      .get
    assertEquals(
      combined.assess(evaluated).map(_.map(_.fee.amount.coefficient)),
      Right(Vector(Rational(-1, 100), Rational(1, 100)))
    )
end DownstreamEconomicsSuite
