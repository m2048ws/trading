package trading.economics.instrument

import munit.FunSuite

class OrderScenarioSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear
  private val lots       = instrument.lots(10).toOption.get
  private val limit      = fixture.price(instrument, 100)
  private val state100   = fixture.state(instrument, 100)

  test(
    "order alternatives expose only locally valid immediate, fixed, trailing, market, priced, and visibility shapes"
  ):
    val fixed    = instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, limit)
    val trailing = instrument.orders
      .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 5)
      .toOption
      .get
    assertEquals(fixed.triggerPrice.ticks.unrefined, BigInt(200))
    assertEquals(trailing.offsetTicks.unrefined, BigInt(5))
    assertEquals(
      instrument.orders.trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 0),
      Left(InvalidTrailingOffset(0))
    )

    val market = instrument.orders.market(Side.Buy, lots).toOption.get
    assert(market.activation.isInstanceOf[ImmediateActivation[?]])
    assert(market.execution.isInstanceOf[MarketExecution[?, ?]])

    val iceberg = instrument.orders.iceberg(instrument.lots(3).toOption.get)
    val priced  = instrument.orders.pricedExecution(
      instrument.orders.limitPricing(limit),
      TimeInForce.GoodTillCancelled,
      LiquidityConstraint.MakerOnly,
      iceberg
    )
    val order = instrument.orders
      .create(instrument.orders.intent(Side.Sell, lots), fixed, priced)
      .toOption
      .get
    assert(order.execution.isInstanceOf[PricedExecution[?, ?, ?]])

    val hidden = instrument.orders.limit(
      Side.Buy,
      lots,
      limit,
      visibility = instrument.orders.hidden
    )
    assert(hidden.isRight)

  test("order aggregate rejects oversized and non-resting iceberg"):
    val oversized          = instrument.orders.iceberg(instrument.lots(11).toOption.get)
    val oversizedExecution = instrument.orders.pricedExecution(
      instrument.orders.limitPricing(limit),
      TimeInForce.Day,
      LiquidityConstraint.Unrestricted,
      oversized
    )
    assertEquals(
      instrument.orders.create(instrument.orders.intent(Side.Buy, lots), instrument.orders.immediate,
        oversizedExecution),
      Left(InvalidOrder(OrderFailureReason.IcebergExceedsOrder(11, 10)))
    )

    val nonResting = instrument.orders.pricedExecution(
      instrument.orders.limitPricing(limit),
      TimeInForce.ImmediateOrCancel,
      LiquidityConstraint.Unrestricted,
      instrument.orders.iceberg(instrument.lots(2).toOption.get)
    )
    assertEquals(
      instrument.orders.create(instrument.orders.intent(Side.Buy, lots), instrument.orders.immediate, nonResting),
      Left(InvalidOrder(OrderFailureReason.NonRestingIceberg))
    )
    assert(NonRestingTimeInForce.from(TimeInForce.Day).isLeft)

  test("complete scenarios consume activation evidence and pricing resolution associated with the order shapes"):
    val marketOrder = instrument.orders.market(Side.Buy, lots).toOption.get
    val taker       = instrument.scenarios.slice(lots, state100, LiquidityRole.Taker).toOption.get
    val direct      = instrument.scenarios.assumptionsOne(marketOrder)(
      marketOrder.activation.evidence,
      marketOrder.execution.resolution,
      taker
    )
    val directScenario = instrument.scenarios.order(marketOrder, direct).toOption.get
    assertEquals(directScenario.effectivePricing, EffectivePricing.Market)

    val fixed            = instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, limit)
    val stop             = instrument.orders.stopMarket(Side.Buy, lots, fixed).toOption.get
    val fixedEvidence    = instrument.orders.fixedEvidence(fixed)(limit).toOption.get
    val fixedAssumptions = instrument.scenarios.assumptionsOne(stop)(
      fixedEvidence,
      stop.execution.resolution,
      taker
    )
    assert(instrument.scenarios.order(stop, fixedAssumptions).isRight)
    val below = fixture.price(instrument, 99)
    assertEquals(
      instrument.orders.fixedEvidence(fixed)(below),
      Left(ActivationViolation.FixedTriggerUnsatisfied)
    )

    val trailing = instrument.orders
      .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrAbove, 2)
      .toOption
      .get
    val trailingOrder       = instrument.orders.stopMarket(Side.Buy, lots, trailing).toOption.get
    val extreme             = fixture.price(instrument, 99)
    val observation         = fixture.price(instrument, 100)
    val evidence            = instrument.orders.trailingEvidence(trailing)(extreme, observation).toOption.get
    val trailingAssumptions = instrument.scenarios.assumptionsOne(trailingOrder)(
      evidence,
      trailingOrder.execution.resolution,
      taker
    )
    assert(instrument.scenarios.order(trailingOrder, trailingAssumptions).isRight)
    val invalidThreshold = instrument.orders
      .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 300)
      .toOption
      .get
    assertEquals(
      instrument.orders.trailingEvidence(invalidThreshold)(extreme, observation),
      Left(ActivationViolation.TrailingThresholdNonPositive)
    )

    val peg             = instrument.orders.peggedPricing(PriceReference.Mark, 2)
    val peggedExecution = instrument.orders.pricedExecution(
      peg,
      TimeInForce.GoodTillCancelled,
      LiquidityConstraint.Unrestricted,
      instrument.orders.displayed
    )
    val peggedOrder = instrument.orders
      .create(instrument.orders.intent(Side.Buy, lots), instrument.orders.immediate, peggedExecution)
      .toOption
      .get
    val reference         = fixture.price(instrument, 99)
    val resolution        = instrument.orders.pegResolution(peg)(reference, limit).toOption.get
    val peggedAssumptions = instrument.scenarios.assumptionsOne(peggedOrder)(
      peggedOrder.activation.evidence,
      resolution,
      taker
    )
    val peggedScenario = instrument.scenarios.order(peggedOrder, peggedAssumptions).toOption.get
    assertEquals(peggedScenario.effectivePricing, EffectivePricing.Limited(limit))
    assertEquals(
      instrument.orders.pegResolution(peg)(reference, fixture.price(instrument, 101)),
      Left(PricingViolation.PegOffsetMismatch(2, 4))
    )

  test("same-shape evidence and resolution remain bound to their activation and pricing semantics"):
    val taker = instrument.scenarios.slice(lots, state100, LiquidityRole.Taker).toOption.get

    val fixed               = instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, limit)
    val fixedEvidence       = instrument.orders.fixedEvidence(fixed)(limit).toOption.get
    val fixedTriggerChanged =
      instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, fixture.price(instrument, 101))
    val fixedReferenceChanged =
      instrument.orders.fixedTrigger(PriceReference.Last, TriggerComparison.AtOrAbove, limit)
    val fixedComparisonChanged =
      instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrBelow, limit)
    assertEquals(fixed.validate(fixedEvidence).map(_.observations.size), Right(1))
    assertEquals(fixedTriggerChanged.validate(fixedEvidence), Left(ActivationViolation.FixedEvidenceMismatch))
    assertEquals(fixedReferenceChanged.validate(fixedEvidence), Left(ActivationViolation.FixedEvidenceMismatch))
    assertEquals(fixedComparisonChanged.validate(fixedEvidence), Left(ActivationViolation.FixedEvidenceMismatch))

    val fixedOrder       = instrument.orders.stopMarket(Side.Buy, lots, fixedTriggerChanged).toOption.get
    val fixedAssumptions = instrument.scenarios.assumptionsOne(fixedOrder)(
      fixedEvidence,
      fixedOrder.execution.resolution,
      taker
    )
    assertEquals(
      instrument.scenarios.order(fixedOrder, fixedAssumptions),
      Left(InvalidScenario(ScenarioFailureReason.FixedEvidenceMismatch))
    )

    val trailing = instrument.orders
      .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, 2)
      .toOption
      .get
    val extreme               = fixture.price(instrument, 99)
    val trailingEvidence      = instrument.orders.trailingEvidence(trailing)(extreme, limit).toOption.get
    val trailingOffsetChanged = instrument.orders
      .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, 3)
      .toOption
      .get
    val trailingReferenceChanged = instrument.orders
      .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrAbove, 2)
      .toOption
      .get
    val trailingComparisonChanged = instrument.orders
      .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrBelow, 2)
      .toOption
      .get
    assertEquals(trailing.validate(trailingEvidence).map(_.observations.size), Right(2))
    assertEquals(
      trailingOffsetChanged.validate(trailingEvidence),
      Left(ActivationViolation.TrailingEvidenceMismatch)
    )
    assertEquals(
      trailingReferenceChanged.validate(trailingEvidence),
      Left(ActivationViolation.TrailingEvidenceMismatch)
    )
    assertEquals(
      trailingComparisonChanged.validate(trailingEvidence),
      Left(ActivationViolation.TrailingEvidenceMismatch)
    )

    val trailingOrder       = instrument.orders.stopMarket(Side.Buy, lots, trailingOffsetChanged).toOption.get
    val trailingAssumptions = instrument.scenarios.assumptionsOne(trailingOrder)(
      trailingEvidence,
      trailingOrder.execution.resolution,
      taker
    )
    assertEquals(
      instrument.scenarios.order(trailingOrder, trailingAssumptions),
      Left(InvalidScenario(ScenarioFailureReason.TrailingEvidenceMismatch))
    )

    val peg                 = instrument.orders.peggedPricing(PriceReference.Mark, 2)
    val pegResolution       = instrument.orders.pegResolution(peg)(extreme, limit).toOption.get
    val pegOffsetChanged    = instrument.orders.peggedPricing(PriceReference.Mark, 3)
    val pegReferenceChanged = instrument.orders.peggedPricing(PriceReference.Last, 2)
    assertEquals(peg.resolve(pegResolution), Right(EffectivePricing.Limited(limit)))
    assertEquals(pegOffsetChanged.resolve(pegResolution), Left(PricingViolation.PegResolutionMismatch))
    assertEquals(pegReferenceChanged.resolve(pegResolution), Left(PricingViolation.PegResolutionMismatch))

    val peggedExecution = instrument.orders.pricedExecution(
      pegOffsetChanged,
      TimeInForce.GoodTillCancelled,
      LiquidityConstraint.Unrestricted,
      instrument.orders.displayed
    )
    val peggedOrder = instrument.orders
      .create(instrument.orders.intent(Side.Buy, lots), instrument.orders.immediate, peggedExecution)
      .toOption
      .get
    val peggedAssumptions = instrument.scenarios.assumptionsOne(peggedOrder)(
      peggedOrder.activation.evidence,
      pegResolution,
      taker
    )
    assertEquals(
      instrument.scenarios.order(peggedOrder, peggedAssumptions),
      Left(InvalidScenario(ScenarioFailureReason.PegResolutionMismatch))
    )

  test("scenario validates exact lot conservation, liquidity roles, limit quality, and round-trip closure"):
    val buyLimit       = instrument.orders.limit(Side.Buy, lots, limit).toOption.get
    val tooExpensive   = fixture.state(instrument, 101)
    val badSlice       = instrument.scenarios.slice(lots, tooExpensive, LiquidityRole.Taker).toOption.get
    val badAssumptions = instrument.scenarios.assumptionsOne(buyLimit)(
      buyLimit.activation.evidence,
      buyLimit.execution.pricing.resolution,
      badSlice
    )
    assertEquals(
      instrument.scenarios.order(buyLimit, badAssumptions),
      Left(InvalidScenario(ScenarioFailureReason.SliceWorseThanLimit, Some(0)))
    )

    val partial = instrument.scenarios
      .slice(instrument.lots(9).toOption.get, state100, LiquidityRole.Taker)
      .toOption
      .get
    val partialAssumptions = instrument.scenarios.assumptionsOne(buyLimit)(
      buyLimit.activation.evidence,
      buyLimit.execution.pricing.resolution,
      partial
    )
    assertEquals(
      instrument.scenarios.order(buyLimit, partialAssumptions),
      Left(InvalidScenario(ScenarioFailureReason.SliceLotsMismatch(10, 9)))
    )

    val makerOnly = instrument.orders
      .limit(Side.Buy, lots, limit, liquidityConstraint = LiquidityConstraint.MakerOnly)
      .toOption
      .get
    val taker          = instrument.scenarios.slice(lots, state100, LiquidityRole.Taker).toOption.get
    val makerViolation = instrument.scenarios.assumptionsOne(makerOnly)(
      makerOnly.activation.evidence,
      makerOnly.execution.pricing.resolution,
      taker
    )
    assertEquals(
      instrument.scenarios.order(makerOnly, makerViolation),
      Left(InvalidScenario(ScenarioFailureReason.MakerOnlySliceNotMaker, Some(0)))
    )

    val entry = fixture.scenario(instrument)(Side.Buy, lots, state100)
    val exit  = fixture.scenario(instrument)(Side.Sell, lots, fixture.state(instrument, 101))
    assertEquals(instrument.scenarios.roundTrip(entry, exit).map(_.heldPosition.count), Right(BigInt(10)))

  test("scenario diagnostics accumulate independent slice violations in stable order and share fail-fast projection"):
    val firstLots  = instrument.lots(4).toOption.get
    val secondLots = instrument.lots(6).toOption.get
    val badMarket  = fixture.state(instrument, 101)
    val first      = instrument.scenarios.slice(firstLots, badMarket, LiquidityRole.Taker).toOption.get
    val second     = instrument.scenarios.slice(secondLots, badMarket, LiquidityRole.Taker).toOption.get
    val order      = instrument.orders
      .limit(Side.Buy, lots, limit, liquidityConstraint = LiquidityConstraint.MakerOnly)
      .toOption
      .get
    val assumptions = instrument.scenarios.assumptionsMany(order)(
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
    assertEquals(instrument.scenarios.diagnose(order, assumptions).left.map(_.violations), Left(expected))
    assertEquals(
      instrument.scenarios.order(order, assumptions),
      Left(InvalidScenario(ScenarioFailureReason.MakerOnlySliceNotMaker, Some(0)))
    )

end OrderScenarioSuite
