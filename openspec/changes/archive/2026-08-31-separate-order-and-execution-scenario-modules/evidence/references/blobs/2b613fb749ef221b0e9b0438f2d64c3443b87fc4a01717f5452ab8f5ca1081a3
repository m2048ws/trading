package trading.order

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*

final class OrderInstructionSuite extends FunSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private val lots     = fixture.lots(instrument, 10)
  private val price95  = fixture.price(instrument, Rational(95))
  private val price99  = fixture.price(instrument, Rational(99))
  private val price100 = fixture.price(instrument, Rational(100))
  private val price101 = fixture.price(instrument, Rational(101))

  test("local duration and trailing refinements reject invalid values"):
    assertEquals(
      NonRestingTimeInForce.from(TimeInForce.ImmediateOrCancel),
      Right(NonRestingTimeInForce.ImmediateOrCancel)
    )
    assertEquals(NonRestingTimeInForce.from(TimeInForce.FillOrKill), Right(NonRestingTimeInForce.FillOrKill))
    assertEquals(
      NonRestingTimeInForce.from(TimeInForce.Day),
      Left(OrderViolation.RestingMarketDuration(TimeInForce.Day))
    )
    assertEquals(
      TrailingActivation.create[B, Q](PriceReference.Mark, TriggerComparison.AtOrAbove, 0),
      Left(OrderViolation.InvalidTrailingOffset(0))
    )
    assert(
      TrailingActivation.create[B, Q](PriceReference.Mark, TriggerComparison.AtOrAbove, 5).isRight
    )

  test("instruction values construct and semantically verify their associated evidence"):
    val fixed         = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
    val fixedEvidence = fixed.evidence(price101).toOption.get
    assert(fixed.verify(fixedEvidence).isRight)
    assertEquals(
      FixedActivation(PriceReference.Last, TriggerComparison.AtOrAbove, price100).verify(fixedEvidence),
      Left(ActivationViolation.FixedEvidenceMismatch)
    )

    val trailing = TrailingActivation
      .create[B, Q](PriceReference.Mark, TriggerComparison.AtOrBelow, 5)
      .toOption
      .get
    val trailingEvidence = trailing.evidence(price100, price95).toOption.get
    assert(trailing.verify(trailingEvidence).isRight)
    assertEquals(
      TrailingActivation
        .create[B, Q](PriceReference.Last, TriggerComparison.AtOrBelow, 5)
        .toOption
        .get
        .verify(trailingEvidence),
      Left(ActivationViolation.TrailingEvidenceMismatch)
    )

    val peg           = PeggedPricing[B, Q](PriceReference.Mark, 2)
    val pegResolution = peg.resolution(price99, price100).toOption.get
    assertEquals(peg.resolve(pegResolution), Right(EffectivePricing.Limited(price100)))
    assertEquals(
      PeggedPricing[B, Q](PriceReference.Last, 2).resolve(pegResolution),
      Left(PricingViolation.PegResolutionMismatch)
    )

  test("every valid activation and execution product remains constructible and exhaustively inspectable"):
    val immediate = ImmediateActivation[B, Q]()
    val fixed     = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
    val trailing  = TrailingActivation
      .create[B, Q](PriceReference.Last, TriggerComparison.AtOrBelow, 1)
      .toOption
      .get
    val activations: Vector[OrderActivation[B, Q]] = Vector(immediate, fixed, trailing)

    val visibilities: Vector[PricedVisibility[D]] =
      Vector(DisplayedVisibility, HiddenVisibility, IcebergVisibility(lots))
    val constraints     = Vector(LiquidityConstraint.Unrestricted, LiquidityConstraint.MakerOnly)
    val durations       = TimeInForce.values.toVector
    val limit           = LimitPricing(price100)
    val peg             = PeggedPricing[B, Q](PriceReference.Mark, 1)
    val limitExecutions =
      for
        duration   <- durations
        constraint <- constraints
        visibility <- visibilities
      yield PricedExecution[D, B, Q, LimitPricing[B, Q]](limit, duration, constraint, visibility)
    val pegExecutions =
      for
        duration   <- durations
        constraint <- constraints
        visibility <- visibilities
      yield PricedExecution[D, B, Q, PeggedPricing[B, Q]](peg, duration, constraint, visibility)
    val marketExecutions: Vector[OrderExecution[D, B, Q]] = NonRestingTimeInForce.values.toVector.map: duration =>
      MarketExecution[D, B, Q](duration)
    val executions: Vector[OrderExecution[D, B, Q]] = marketExecutions ++ limitExecutions ++ pegExecutions

    def activationName(value: OrderActivation[B, Q]): String =
      value match
        case _: ImmediateActivation[?, ?] => "immediate"
        case FixedActivation(_, _, _)     => "fixed"
        case TrailingActivation(_, _, _)  => "trailing"

    def executionName(value: OrderExecution[D, B, Q]): String =
      value match
        case MarketExecution(_)                               => "market"
        case PricedExecution(LimitPricing(_), _, _, _)        => "limit"
        case PricedExecution(_: PeggedPricing[?, ?], _, _, _) => "peg"

    assertEquals(activations.map(activationName), Vector("immediate", "fixed", "trailing"))
    assertEquals(executions.size, 50)
    assertEquals(executions.map(executionName).groupMapReduce(identity)(_ => 1)(_ + _),
      Map("market" -> 2, "limit" -> 24, "peg" -> 24))
    assertEquals(activations.flatMap(activation => executions.map(activation -> _)).size, 150)

  test("canonical intent and convenience constructors retain signed position and reduce-only data"):
    val buyIntent  = OrderIntent.create(instrument)(Side.Buy, lots).toOption.get
    val sellIntent = OrderIntent
      .create(instrument)(Side.Sell, lots, PositionEffect.ReduceOnly)
      .toOption
      .get
    assertEquals(buyIntent.positionChange.coordinate, BigInt(10))
    assertEquals(sellIntent.positionChange.coordinate, BigInt(-10))
    assertEquals(sellIntent.positionEffect, PositionEffect.ReduceOnly)

    val market     = Order.market(instrument)(Side.Buy, lots).toOption.get
    val limit      = Order.limit(instrument)(Side.Sell, lots, price100).toOption.get
    val fixed      = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
    val stopMarket = Order.stopMarket(instrument)(Side.Buy, lots, fixed).toOption.get
    val stopLimit  = Order.stopLimit(instrument)(Side.Sell, lots, fixed, price100).toOption.get
    assertEquals(market.intent.positionChange.coordinate, BigInt(10))
    assertEquals(limit.intent.positionChange.coordinate, BigInt(-10))
    assertEquals(stopMarket.activation, fixed)
    assertEquals(stopLimit.activation, fixed)

  test("canonical validation accumulates independent iceberg violations and fail-fast projects the head"):
    val intent       = OrderIntent.create(instrument)(Side.Buy, lots).toOption.get
    val oversized    = fixture.lots(instrument, 11)
    val immediate    = ImmediateActivation[B, Q]()
    val badExecution = PricedExecution[D, B, Q, LimitPricing[B, Q]](
      LimitPricing(price100),
      TimeInForce.ImmediateOrCancel,
      LiquidityConstraint.Unrestricted,
      IcebergVisibility(oversized)
    )
    val expected = Vector(
      OrderViolation.IcebergExceedsOrder(11, 10),
      OrderViolation.NonRestingIceberg
    )
    assertEquals(
      Order.create(instrument)(intent, immediate, badExecution).left.map(_.violations),
      Left(expected)
    )
    assertEquals(
      Order.createFirst(instrument)(intent, immediate, badExecution),
      Left(OrderViolation.IcebergExceedsOrder(11, 10))
    )

  test("identity validation uses closed locations, stable order, and suppresses dependent iceberg rules"):
    val foreign       = fixture.foreignIdentity
    val foreignLots   = fixture.lots(foreign, 12)
    val foreignIntent = OrderIntent
      .create(foreign)(Side.Buy, foreignLots)
      .toOption
      .get
    type FD = foreign.roles.position.D
    type FB = foreign.roles.base.D
    type FQ = foreign.roles.quote.D

    val foreignIntentResult = Order.create(instrument)(
      foreignIntent,
      ImmediateActivation[B, Q](),
      MarketExecution[FD, B, Q](NonRestingTimeInForce.ImmediateOrCancel)
    )
    assertEquals(
      foreignIntentResult.left.map(_.violations),
      Left(
        Vector(
          OrderViolation.InstrumentMismatch(
            OrderComponent.Intent,
            instrument.identity.id,
            foreign.identity.id
          ),
          OrderViolation.InstrumentMismatch(
            OrderComponent.Lots,
            instrument.identity.id,
            foreign.identity.id
          )
        )
      )
    )

    val foreignPrice   = fixture.price(foreign, Rational(100))
    val targetIntent   = OrderIntent.create(instrument)(Side.Buy, lots).toOption.get
    val foreignTrigger = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, foreignPrice)
    val triggerResult  = Order.create(instrument)(
      targetIntent,
      foreignTrigger,
      MarketExecution[D, FB, FQ](NonRestingTimeInForce.ImmediateOrCancel)
    )
    assertEquals(
      triggerResult.left.map(_.violations),
      Left(
        Vector(
          OrderViolation.InstrumentMismatch(
            OrderComponent.TriggerPrice,
            instrument.identity.id,
            foreign.identity.id
          )
        )
      )
    )

    val foreignDisplayedExecution = PricedExecution[FD, B, Q, LimitPricing[B, Q]](
      LimitPricing(price100),
      TimeInForce.ImmediateOrCancel,
      LiquidityConstraint.Unrestricted,
      IcebergVisibility(foreignLots)
    )
    assertEquals(
      Order
        .create(instrument)(
          foreignIntent,
          ImmediateActivation[B, Q](),
          foreignDisplayedExecution
        )
        .left
        .map(_.violations),
      Left(
        Vector(
          OrderViolation.InstrumentMismatch(
            OrderComponent.Intent,
            instrument.identity.id,
            foreign.identity.id
          ),
          OrderViolation.InstrumentMismatch(
            OrderComponent.Lots,
            instrument.identity.id,
            foreign.identity.id
          ),
          OrderViolation.InstrumentMismatch(
            OrderComponent.DisplayedLots,
            instrument.identity.id,
            foreign.identity.id
          )
        )
      )
    )
end OrderInstructionSuite
