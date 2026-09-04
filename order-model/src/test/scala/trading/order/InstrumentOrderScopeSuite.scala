package trading.order

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*

final class InstrumentOrderScopeSuite extends FunSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear
  private val lots       = fixture.lots(instrument, 10)
  private val limitPrice = fixture.price(instrument, Rational(100))

  private def assertBehaviorallyEqual[D <: Dim, B <: Dim, Q <: Dim](
    actual: Either[OrderViolations, Order[D, B, Q]],
    expected: Either[OrderViolations, Order[D, B, Q]]
  ): Unit =
    (actual, expected) match
      case (Left(actualErrors), Left(expectedErrors)) =>
        assertEquals(actualErrors, expectedErrors)
      case (Right(actualOrder), Right(expectedOrder)) =>
        assertEquals(actualOrder.instrumentId, expectedOrder.instrumentId)
        assertEquals(actualOrder.intent, expectedOrder.intent)
        val actualActivation: OrderActivation[B, Q]    = actualOrder.activation
        val expectedActivation: OrderActivation[B, Q]  = expectedOrder.activation
        val actualExecution: OrderExecution[D, B, Q]   = actualOrder.execution
        val expectedExecution: OrderExecution[D, B, Q] = expectedOrder.execution
        (actualActivation, expectedActivation) match
          case (_: ImmediateActivation[?, ?], _: ImmediateActivation[?, ?]) => ()
          case (FixedActivation(leftReference, leftComparison, leftPrice),
              FixedActivation(rightReference, rightComparison, rightPrice)) =>
            assertEquals(leftReference, rightReference)
            assertEquals(leftComparison, rightComparison)
            assertEquals(leftPrice, rightPrice)
          case (TrailingActivation(leftReference, leftComparison, leftTicks),
              TrailingActivation(rightReference, rightComparison, rightTicks)) =>
            assertEquals(leftReference, rightReference)
            assertEquals(leftComparison, rightComparison)
            assertEquals(leftTicks, rightTicks)
          case _ => fail(s"different activations: $actualActivation, $expectedActivation")
        assertEquals(actualExecution, expectedExecution)
        assert(actualOrder.ne(expectedOrder), "scope must construct an independent immutable order")
      case _ => fail(s"different scoped and direct outcomes: $actual, $expected")

  test("instrument scope preserves all standard constructor refinements, defaults, and explicit options"):
    val orders = Order.forInstrument(instrument)
    val fixed  = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, limitPrice)
    val shown: PricedVisibility[instrument.PositionD] =
      IcebergVisibility(fixture.lots(instrument, 5))

    val scopedMarket: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        ImmediateActivation[instrument.BaseD, instrument.QuoteD],
        MarketExecution[instrument.PositionD, instrument.BaseD, instrument.QuoteD]
      ]
    ]                = orders.market(Side.Buy, lots)
    val directMarket = Order.market(instrument)(Side.Buy, lots)
    assertBehaviorallyEqual(scopedMarket, directMarket)
    assertBehaviorallyEqual(
      orders.limit(Side.Buy, lots, limitPrice),
      Order.limit(instrument)(Side.Buy, lots, limitPrice)
    )

    val scopedLimit: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        ImmediateActivation[instrument.BaseD, instrument.QuoteD],
        PricedExecution[
          instrument.PositionD,
          instrument.BaseD,
          instrument.QuoteD,
          LimitPricing[instrument.BaseD, instrument.QuoteD]
        ]
      ]
    ] = orders.limit(
      Side.Sell,
      lots,
      limitPrice,
      TimeInForce.Day,
      LiquidityConstraint.MakerOnly,
      PositionEffect.ReduceOnly,
      shown
    )
    val directLimit = Order.limit(instrument)(
      Side.Sell,
      lots,
      limitPrice,
      TimeInForce.Day,
      LiquidityConstraint.MakerOnly,
      PositionEffect.ReduceOnly,
      shown
    )
    assertBehaviorallyEqual(scopedLimit, directLimit)

    val scopedStopMarket: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        fixed.type,
        MarketExecution[instrument.PositionD, instrument.BaseD, instrument.QuoteD]
      ]
    ]                    = orders.stopMarket(Side.Buy, lots, fixed)
    val directStopMarket = Order.stopMarket(instrument)(Side.Buy, lots, fixed)
    assertBehaviorallyEqual(scopedStopMarket, directStopMarket)

    val scopedStopLimit: Either[
      OrderViolations,
      Order.Aux[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        fixed.type,
        PricedExecution[
          instrument.PositionD,
          instrument.BaseD,
          instrument.QuoteD,
          LimitPricing[instrument.BaseD, instrument.QuoteD]
        ]
      ]
    ]                   = orders.stopLimit(Side.Sell, lots, fixed, limitPrice)
    val directStopLimit = Order.stopLimit(instrument)(Side.Sell, lots, fixed, limitPrice)
    assertBehaviorallyEqual(scopedStopLimit, directStopLimit)

    val limitOrder = scopedLimit.toOption.get
    assertEquals(limitOrder.intent.positionEffect, PositionEffect.ReduceOnly)
    assertEquals(limitOrder.intent.positionChange.coordinate, BigInt(-10))
    assertEquals(limitOrder.execution.timeInForce, TimeInForce.Day)
    assertEquals(limitOrder.execution.liquidityConstraint, LiquidityConstraint.MakerOnly)
    assertEquals(limitOrder.execution.visibility, shown)

  test("instrument scope preserves complete deterministic validation and generic foreign checks"):
    val orders    = Order.forInstrument(instrument)
    val oversized = fixture.lots(instrument, 11)
    val invalid   = orders.limit(
      Side.Buy,
      lots,
      limitPrice,
      timeInForce = TimeInForce.ImmediateOrCancel,
      visibility = IcebergVisibility(oversized)
    )
    val direct = Order.limit(instrument)(
      Side.Buy,
      lots,
      limitPrice,
      timeInForce = TimeInForce.ImmediateOrCancel,
      visibility = IcebergVisibility(oversized)
    )
    val expected = Vector(
      OrderViolation.IcebergExceedsOrder(11, 10),
      OrderViolation.NonRestingIceberg
    )
    assertEquals(invalid.left.map(_.violations), Left(expected))
    assertBehaviorallyEqual(invalid, direct)

    val foreign       = fixture.foreignIdentity
    val foreignLots   = fixture.lots(foreign, 12)
    val foreignIntent = OrderIntent.create(foreign)(Side.Buy, foreignLots).toOption.get
    val generic       = Order.create(instrument)(
      foreignIntent,
      ImmediateActivation[instrument.BaseD, instrument.QuoteD](),
      MarketExecution[foreign.PositionD, instrument.BaseD, instrument.QuoteD](
        NonRestingTimeInForce.ImmediateOrCancel
      )
    )
    assertEquals(
      generic.left.map(_.violations),
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

  test("one immutable scope is independent under repeated, reordered, and concurrent use"):
    val orders = Order.forInstrument(instrument)

    val firstMarket  = orders.market(Side.Buy, lots)
    val firstLimit   = orders.limit(Side.Sell, lots, limitPrice)
    val secondLimit  = orders.limit(Side.Sell, lots, limitPrice)
    val secondMarket = orders.market(Side.Buy, lots)
    assertBehaviorallyEqual(firstMarket, secondMarket)
    assertBehaviorallyEqual(firstLimit, secondLimit)

    val concurrent = Await.result(
      Future.sequence(Vector.fill(32)(Future(orders.market(Side.Buy, lots)))),
      10.seconds
    )
    concurrent.foreach(result => assertBehaviorallyEqual(result, Order.market(instrument)(Side.Buy, lots)))
    val successful = concurrent.flatMap(_.toOption)
    assertEquals(successful.size, 32)
    assert(
      successful.combinations(2).forall:
        case Vector(left, right) => left.ne(right)
        case _                   => true
    )
end InstrumentOrderScopeSuite
