package trading.economics

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
    assert(market.activation.isInstanceOf[ImmediateActivation[?, ?]])
    assert(market.execution.isInstanceOf[MarketExecution[?, ?, ?]])

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

  test("order aggregate rejects oversized and non-resting iceberg and invalid stop convenience"):
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
    assertEquals(
      instrument.orders.stopMarket(Side.Buy, lots, instrument.orders.immediate),
      Left(InvalidOrder(OrderFailureReason.StopRequiresTrigger))
    )
    assert(NonRestingTimeInForce.from(TimeInForce.Day).isLeft)

  test("complete scenarios validate immediate/fixed/trailing evidence and direct/pegged pricing"):
    val marketOrder = instrument.orders.market(Side.Buy, lots).toOption.get
    val taker       = instrument.scenarios.slice(lots, state100, LiquidityRole.Taker)
    val direct      = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.directPricing,
      Vector(taker)
    )
    assert(instrument.scenarios.order(marketOrder, direct).isRight)

    val fixed            = instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, limit)
    val stop             = instrument.orders.stopMarket(Side.Buy, lots, fixed).toOption.get
    val fixedEvidence    = instrument.scenarios.fixedEvidence(PriceReference.Mark, limit)
    val fixedAssumptions = instrument.scenarios.assumptions(
      instrument.scenarios.triggered(fixedEvidence),
      instrument.scenarios.directPricing,
      Vector(taker)
    )
    assert(instrument.scenarios.order(stop, fixedAssumptions).isRight)
    assertEquals(
      instrument.scenarios.order(stop, direct),
      Left(InvalidScenario(ScenarioFailureReason.MissingFixedTriggerEvidence))
    )
    val extraneous = instrument.scenarios.assumptions(
      instrument.scenarios.triggered(fixedEvidence),
      instrument.scenarios.directPricing,
      Vector(taker)
    )
    assertEquals(
      instrument.scenarios.order(marketOrder, extraneous),
      Left(InvalidScenario(ScenarioFailureReason.UnexpectedTriggerEvidence))
    )

    val trailing = instrument.orders
      .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrAbove, 2)
      .toOption
      .get
    val trailingOrder       = instrument.orders.stopMarket(Side.Buy, lots, trailing).toOption.get
    val extreme             = instrument.prices.exact(trading.quantity.Rational(99)).toOption.get
    val observation         = instrument.prices.exact(trading.quantity.Rational(100)).toOption.get
    val evidence            = instrument.scenarios.trailingEvidence(PriceReference.Last, extreme, observation)
    val trailingAssumptions = instrument.scenarios.assumptions(
      instrument.scenarios.triggered(evidence),
      instrument.scenarios.directPricing,
      Vector(taker)
    )
    assert(instrument.scenarios.order(trailingOrder, trailingAssumptions).isRight)

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
    val reference         = instrument.prices.exact(trading.quantity.Rational(99)).toOption.get
    val resolution        = instrument.scenarios.pegResolution(PriceReference.Mark, reference, limit)
    val peggedAssumptions = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.resolvedPeg(resolution),
      Vector(taker)
    )
    assert(instrument.scenarios.order(peggedOrder, peggedAssumptions).isRight)
    val wrongResolution = instrument.scenarios.pegResolution(
      PriceReference.Mark,
      reference,
      instrument.prices.exact(trading.quantity.Rational(101)).toOption.get
    )
    val wrongPeg = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.resolvedPeg(wrongResolution),
      Vector(taker)
    )
    assertEquals(
      instrument.scenarios.order(peggedOrder, wrongPeg),
      Left(InvalidScenario(ScenarioFailureReason.PegOffsetMismatch))
    )

  test("scenario validates exact lot conservation, liquidity roles, limit quality, and round-trip closure"):
    val buyLimit       = instrument.orders.limit(Side.Buy, lots, limit).toOption.get
    val tooExpensive   = fixture.state(instrument, 101)
    val badSlice       = instrument.scenarios.slice(lots, tooExpensive, LiquidityRole.Taker)
    val badAssumptions = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.directPricing,
      Vector(badSlice)
    )
    assertEquals(
      instrument.scenarios.order(buyLimit, badAssumptions),
      Left(InvalidScenario(ScenarioFailureReason.SliceWorseThanLimit, Some(0)))
    )

    val partial            = instrument.scenarios.slice(instrument.lots(9).toOption.get, state100, LiquidityRole.Taker)
    val partialAssumptions = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.directPricing,
      Vector(partial)
    )
    assertEquals(
      instrument.scenarios.order(buyLimit, partialAssumptions),
      Left(InvalidScenario(ScenarioFailureReason.SliceLotsMismatch(10, 9)))
    )

    val makerOnly = instrument.orders
      .limit(
        Side.Buy,
        lots,
        limit,
        liquidityConstraint = LiquidityConstraint.MakerOnly
      )
      .toOption
      .get
    val taker          = instrument.scenarios.slice(lots, state100, LiquidityRole.Taker)
    val makerViolation = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.directPricing,
      Vector(taker)
    )
    assertEquals(
      instrument.scenarios.order(makerOnly, makerViolation),
      Left(InvalidScenario(ScenarioFailureReason.MakerOnlySliceNotMaker, Some(0)))
    )

    val entry = fixture.scenario(instrument)(Side.Buy, lots, state100)
    val exit  = fixture.scenario(instrument)(Side.Sell, lots, fixture.state(instrument, 101))
    assertEquals(instrument.scenarios.roundTrip(entry, exit).map(_.heldPosition.count), Right(BigInt(10)))

end OrderScenarioSuite
