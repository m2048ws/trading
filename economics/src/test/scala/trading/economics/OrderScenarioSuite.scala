package trading.economics

import munit.FunSuite

class OrderScenarioSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  test("order vocabulary retains every independent instruction axis"):
    val lots      = instrument.lots(10).toOption.get
    val displayed = instrument.orders.icebergVisibility(instrument.lots(3).toOption.get)
    val order     = instrument
      .orders.limit(
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

    val makerMarket = instrument.orders.checked(
      Side.Buy,
      lots,
      instrument.orders.immediateActivation,
      instrument.orders.marketPriceInstruction,
      TimeInForce.ImmediateOrCancel,
      LiquidityConstraint.MakerOnly,
      PositionEffect.Unrestricted,
      instrument.orders.notApplicableVisibility
    )
    val restingMarket = instrument.orders.checked(
      Side.Buy,
      lots,
      instrument.orders.immediateActivation,
      instrument.orders.marketPriceInstruction,
      TimeInForce.GoodTillCancelled,
      LiquidityConstraint.Unrestricted,
      PositionEffect.Unrestricted,
      instrument.orders.notApplicableVisibility
    )
    val nonRestingIceberg = instrument.orders.limit(
      Side.Buy,
      lots,
      fixture.price(instrument, 100),
      TimeInForce.ImmediateOrCancel,
      visibility = instrument.orders.icebergVisibility(instrument.lots(2).toOption.get)
    )

    assert(makerMarket.isLeft)
    assert(restingMarket.isLeft)
    assert(nonRestingIceberg.isLeft)

  test("fixed trigger evidence is distinct from the matched market state"):
    val lots    = instrument.lots(10).toOption.get
    val trigger = instrument.orders.fixedTrigger(
      PriceReference.Mark,
      TriggerComparison.AtOrBelow,
      fixture.price(instrument, 100)
    )
    val order        = instrument.orders.stopMarket(Side.Sell, lots, trigger).toOption.get
    val matchedState = instrument.market.quoteSettled(fixture.price(instrument, 90)).toOption.get
    val slice        = instrument.scenarios.slice(lots, matchedState, LiquidityRole.Taker)
    val evidence     = instrument.scenarios.fixedTriggerEvidence(PriceReference.Mark, fixture.price(instrument, 99))

    assert(instrument.scenarios.order(order, Vector(slice)).isLeft)
    val scenario = instrument.scenarios.order(order, Vector(slice), Some(evidence)).toOption.get
    assertEquals(instrument.prices.ticks(scenario.activationEvidence.get.observedPrice), BigInt(198))
    assertEquals(instrument.prices.ticks(scenario.slices.head.market.price), BigInt(180))

  test("trailing triggers derive a checked tick threshold"):
    val lots    = instrument.lots(10).toOption.get
    val trigger = instrument
      .orders.trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 4)
      .toOption
      .get
    val order    = instrument.orders.stopMarket(Side.Sell, lots, trigger).toOption.get
    val market   = instrument.market.quoteSettled(fixture.price(instrument, 98)).toOption.get
    val slice    = instrument.scenarios.slice(lots, market, LiquidityRole.Taker)
    val evidence = instrument.scenarios.trailingTriggerEvidence(
      PriceReference.Last,
      fixture.price(instrument, 100),
      fixture.price(instrument, 98)
    )

    assert(instrument.scenarios.order(order, Vector(slice), Some(evidence)).isRight)
    assert(instrument.orders.trailingTrigger(PriceReference.Last, TriggerComparison.AtOrBelow, 0).isLeft)

  test("peg resolution validates the exact signed tick difference"):
    val lots        = instrument.lots(10).toOption.get
    val instruction = instrument.orders.peggedPriceInstruction(PriceReference.Mark, -2)
    val order       = instrument
      .orders.checked(
        Side.Buy,
        lots,
        instrument.orders.immediateActivation,
        instruction,
        TimeInForce.GoodTillCancelled,
        LiquidityConstraint.Unrestricted,
        PositionEffect.Unrestricted,
        instrument.orders.displayedVisibility
      )
      .toOption
      .get
    val state = instrument.market.quoteSettled(fixture.price(instrument, 99)).toOption.get
    val slice = instrument.scenarios.slice(lots, state, LiquidityRole.Maker)
    val valid = instrument.scenarios.pegResolution(
      PriceReference.Mark,
      fixture.price(instrument, 100),
      fixture.price(instrument, 99)
    )
    val invalid = instrument.scenarios.pegResolution(
      PriceReference.Mark,
      fixture.price(instrument, 100),
      fixture.price(instrument, 98)
    )

    assert(instrument.scenarios.order(order, Vector(slice), pegResolution = Some(valid)).isRight)
    assert(instrument.scenarios.order(order, Vector(slice), pegResolution = Some(invalid)).isLeft)

  test("complete scenarios enforce role, limit, and exact lot conservation"):
    val lots  = instrument.lots(10).toOption.get
    val order = instrument.orders.limit(Side.Buy, lots, fixture.price(instrument, 100)).toOption.get
    val first = instrument.scenarios.slice(
      instrument.lots(4).toOption.get,
      instrument.market.quoteSettled(fixture.price(instrument, 99)).toOption.get,
      LiquidityRole.Taker
    )
    val second = instrument.scenarios.slice(
      instrument.lots(6).toOption.get,
      instrument.market.quoteSettled(fixture.price(instrument, 100)).toOption.get,
      LiquidityRole.Maker
    )
    val tooExpensive = instrument.scenarios.slice(
      instrument.lots(6).toOption.get,
      instrument.market.quoteSettled(fixture.price(instrument, 101)).toOption.get,
      LiquidityRole.Maker
    )

    val complete = instrument.scenarios.order(order, Vector(first, second)).toOption.get
    assertEquals(complete.slices.map(slice => instrument.lotCount(slice.lots)), Vector(BigInt(4), BigInt(6)))
    assert(instrument.scenarios.order(order, Vector(first)).isLeft)
    assert(instrument.scenarios.order(order, Vector(first, tooExpensive)).isLeft)

    val marketOrder = instrument.orders.market(Side.Buy, lots).toOption.get
    assert(instrument.scenarios.order(marketOrder, Vector(first, second)).isLeft)

  test("maker-only complete scenarios accept only maker slices"):
    val lots  = instrument.lots(10).toOption.get
    val order = instrument
      .orders.limit(
        Side.Buy,
        lots,
        fixture.price(instrument, 100),
        liquidityConstraint = LiquidityConstraint.MakerOnly
      )
      .toOption
      .get
    val market = instrument.market.quoteSettled(fixture.price(instrument, 100)).toOption.get
    val maker  = instrument.scenarios.slice(lots, market, LiquidityRole.Maker)
    val taker  = instrument.scenarios.slice(lots, market, LiquidityRole.Taker)

    assert(instrument.scenarios.order(order, Vector(maker)).isRight)
    assert(instrument.scenarios.order(order, Vector(taker)).isLeft)

  test("round trips preserve both legs and require exact flatness"):
    val lots      = instrument.lots(10).toOption.get
    val entry     = completeMarket(Side.Buy, lots, 100)
    val exit      = completeMarket(Side.Sell, lots, 110)
    val roundTrip = instrument.scenarios.roundTrip(entry, exit).toOption.get

    assertEquals(instrument.positionLotCount(roundTrip.heldPosition), BigInt(10))
    assertEquals(roundTrip.entry.slices.size, 1)
    assertEquals(roundTrip.exit.slices.size, 1)

    val smallerExit = completeMarket(Side.Sell, instrument.lots(9).toOption.get, 110)
    assert(instrument.scenarios.roundTrip(entry, smallerExit).isLeft)

  private def completeMarket(side: Side, lots: instrument.Lots, dollars: BigInt): instrument.OrderScenario =
    val order  = instrument.orders.market(side, lots).toOption.get
    val market = instrument.market.quoteSettled(fixture.price(instrument, dollars)).toOption.get
    val slice  = instrument.scenarios.slice(lots, market, LiquidityRole.Taker)
    instrument.scenarios.order(order, Vector(slice)).toOption.get

end OrderScenarioSuite
