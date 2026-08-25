package trading.economics

import munit.FunSuite

class OrderScenarioSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  test("order vocabulary retains every independent instruction axis"):
    val lots      = instrument.lots(10).toOption.get
    val displayed = instrument.icebergVisibility(instrument.lots(3).toOption.get)
    val order     = instrument
      .limitOrder(
        Side.Sell,
        lots,
        fixture.price(instrument, 100),
        TimeInForce.Day,
        LiquidityConstraint.MakerOnly,
        PositionEffect.ReduceOnly,
        displayed
      )
      .toOption
      .get

    assertEquals(order.side, Side.Sell)
    assertEquals(order.timeInForce, TimeInForce.Day)
    assertEquals(order.liquidityConstraint, LiquidityConstraint.MakerOnly)
    assertEquals(order.positionEffect, PositionEffect.ReduceOnly)
    assertEquals(order.visibility.kind, VisibilityKind.Iceberg)
    assertEquals(order.priceInstruction.kind, PriceInstructionKind.Limit)
    assertEquals(order.activation.kind, ActivationKind.Immediate)

  test("universal compatibility checks reject impossible market and iceberg combinations"):
    val lots = instrument.lots(10).toOption.get

    val makerMarket = instrument.order(
      Side.Buy,
      lots,
      instrument.immediateActivation,
      instrument.marketPriceInstruction,
      TimeInForce.ImmediateOrCancel,
      LiquidityConstraint.MakerOnly,
      PositionEffect.Unrestricted,
      instrument.notApplicableVisibility
    )
    val restingMarket = instrument.order(
      Side.Buy,
      lots,
      instrument.immediateActivation,
      instrument.marketPriceInstruction,
      TimeInForce.GoodTillCancelled,
      LiquidityConstraint.Unrestricted,
      PositionEffect.Unrestricted,
      instrument.notApplicableVisibility
    )
    val nonRestingIceberg = instrument.limitOrder(
      Side.Buy,
      lots,
      fixture.price(instrument, 100),
      TimeInForce.ImmediateOrCancel,
      visibility = instrument.icebergVisibility(instrument.lots(2).toOption.get)
    )

    assert(makerMarket.isLeft)
    assert(restingMarket.isLeft)
    assert(nonRestingIceberg.isLeft)

  test("fixed trigger evidence is distinct from the matched market state"):
    val lots    = instrument.lots(10).toOption.get
    val trigger = instrument.fixedTrigger(
      PriceReference.Mark,
      TriggerComparison.AtOrBelow,
      fixture.price(instrument, 100)
    )
    val order        = instrument.stopMarketOrder(Side.Sell, lots, trigger).toOption.get
    val matchedState = instrument.marketStateForQuote(fixture.price(instrument, 90)).toOption.get
    val slice        = instrument.liquiditySlice(lots, matchedState, LiquidityRole.Taker)
    val evidence     = instrument.fixedTriggerEvidence(PriceReference.Mark, fixture.price(instrument, 99))

    assert(instrument.orderScenario(order, Vector(slice)).isLeft)
    val scenario = instrument.orderScenario(order, Vector(slice), Some(evidence)).toOption.get
    assertEquals(instrument.priceCoordinate(scenario.activationEvidence.get.observedPrice), BigInt(198))
    assertEquals(instrument.priceCoordinate(scenario.slices.head.market.price), BigInt(180))

  test("trailing triggers derive a checked tick threshold"):
    val lots    = instrument.lots(10).toOption.get
    val trigger = instrument
      .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 4)
      .toOption
      .get
    val order    = instrument.stopMarketOrder(Side.Sell, lots, trigger).toOption.get
    val market   = instrument.marketStateForQuote(fixture.price(instrument, 98)).toOption.get
    val slice    = instrument.liquiditySlice(lots, market, LiquidityRole.Taker)
    val evidence = instrument.trailingTriggerEvidence(
      PriceReference.Last,
      fixture.price(instrument, 100),
      fixture.price(instrument, 98)
    )

    assert(instrument.orderScenario(order, Vector(slice), Some(evidence)).isRight)
    assert(instrument.trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 0).isLeft)

  test("peg resolution validates the exact signed tick difference"):
    val lots        = instrument.lots(10).toOption.get
    val instruction = instrument.peggedPriceInstruction(PriceReference.Mark, -2)
    val order       = instrument
      .order(
        Side.Buy,
        lots,
        instrument.immediateActivation,
        instruction,
        TimeInForce.GoodTillCancelled,
        LiquidityConstraint.Unrestricted,
        PositionEffect.Unrestricted,
        instrument.displayedVisibility
      )
      .toOption
      .get
    val state = instrument.marketStateForQuote(fixture.price(instrument, 99)).toOption.get
    val slice = instrument.liquiditySlice(lots, state, LiquidityRole.Maker)
    val valid = instrument.pegResolution(
      PriceReference.Mark,
      fixture.price(instrument, 100),
      fixture.price(instrument, 99)
    )
    val invalid = instrument.pegResolution(
      PriceReference.Mark,
      fixture.price(instrument, 100),
      fixture.price(instrument, 98)
    )

    assert(instrument.orderScenario(order, Vector(slice), pegResolution = Some(valid)).isRight)
    assert(instrument.orderScenario(order, Vector(slice), pegResolution = Some(invalid)).isLeft)

  test("complete scenarios enforce role, limit, and exact lot conservation"):
    val lots  = instrument.lots(10).toOption.get
    val order = instrument.limitOrder(Side.Buy, lots, fixture.price(instrument, 100)).toOption.get
    val first = instrument.liquiditySlice(
      instrument.lots(4).toOption.get,
      instrument.marketStateForQuote(fixture.price(instrument, 99)).toOption.get,
      LiquidityRole.Taker
    )
    val second = instrument.liquiditySlice(
      instrument.lots(6).toOption.get,
      instrument.marketStateForQuote(fixture.price(instrument, 100)).toOption.get,
      LiquidityRole.Maker
    )
    val tooExpensive = instrument.liquiditySlice(
      instrument.lots(6).toOption.get,
      instrument.marketStateForQuote(fixture.price(instrument, 101)).toOption.get,
      LiquidityRole.Maker
    )

    val complete = instrument.orderScenario(order, Vector(first, second)).toOption.get
    assertEquals(complete.slices.map(slice => instrument.lotCount(slice.lots)), Vector(BigInt(4), BigInt(6)))
    assert(instrument.orderScenario(order, Vector(first)).isLeft)
    assert(instrument.orderScenario(order, Vector(first, tooExpensive)).isLeft)

    val marketOrder = instrument.marketOrder(Side.Buy, lots).toOption.get
    assert(instrument.orderScenario(marketOrder, Vector(first, second)).isLeft)

  test("maker-only complete scenarios accept only maker slices"):
    val lots  = instrument.lots(10).toOption.get
    val order = instrument
      .limitOrder(
        Side.Buy,
        lots,
        fixture.price(instrument, 100),
        liquidityConstraint = LiquidityConstraint.MakerOnly
      )
      .toOption
      .get
    val market = instrument.marketStateForQuote(fixture.price(instrument, 100)).toOption.get
    val maker  = instrument.liquiditySlice(lots, market, LiquidityRole.Maker)
    val taker  = instrument.liquiditySlice(lots, market, LiquidityRole.Taker)

    assert(instrument.orderScenario(order, Vector(maker)).isRight)
    assert(instrument.orderScenario(order, Vector(taker)).isLeft)

  test("round trips preserve both legs and require exact flatness"):
    val lots      = instrument.lots(10).toOption.get
    val entry     = completeMarket(Side.Buy, lots, 100)
    val exit      = completeMarket(Side.Sell, lots, 110)
    val roundTrip = instrument.roundTrip(entry, exit).toOption.get

    assertEquals(instrument.positionLotCount(roundTrip.heldPosition), BigInt(10))
    assertEquals(roundTrip.entry.slices.size, 1)
    assertEquals(roundTrip.exit.slices.size, 1)

    val smallerExit = completeMarket(Side.Sell, instrument.lots(9).toOption.get, 110)
    assert(instrument.roundTrip(entry, smallerExit).isLeft)

  private def completeMarket(side: Side, lots: instrument.Lots, dollars: BigInt): instrument.OrderScenario =
    val order  = instrument.marketOrder(side, lots).toOption.get
    val market = instrument.marketStateForQuote(fixture.price(instrument, dollars)).toOption.get
    val slice  = instrument.liquiditySlice(lots, market, LiquidityRole.Taker)
    instrument.orderScenario(order, Vector(slice)).toOption.get

end OrderScenarioSuite
