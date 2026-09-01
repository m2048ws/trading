package trading.scenario

import munit.FunSuite

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*

final class OrderScenarioSuite extends FunSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private val lots5    = fixture.lots(instrument, 5)
  private val lots9    = fixture.lots(instrument, 9)
  private val lots10   = fixture.lots(instrument, 10)
  private val price99  = fixture.price(instrument, Rational(99))
  private val price100 = fixture.price(instrument, Rational(100))

  private def slice(
    lots: instrument.Lots,
    price: Rational,
    role: LiquidityRole
  ): LiquiditySlice[instrument.Lots, instrument.MarketState] =
    LiquiditySlice
      .create(instrument)(lots, fixture.quoteState(instrument, price), role)
      .toOption
      .get

  test("successful evaluation retains one order, verified results, slices, and intent position change"):
    val order       = Order.limit(instrument)(Side.Buy, lots10, price100).toOption.get
    val first       = slice(lots5, Rational(99), LiquidityRole.Maker)
    val second      = slice(lots5, Rational(100), LiquidityRole.Taker)
    val assumptions = ScenarioAssumptions.many(order)(
      order.activation.evidence,
      order.execution.pricing.resolution,
      first,
      second
    )

    val evaluated = OrderScenario.evaluate(instrument)(assumptions).toOption.get
    assertEquals(evaluated.order, order)
    assertEquals(evaluated.assumptions, assumptions)
    assertEquals(evaluated.matchedSlices.toVector, Vector(first, second))
    assertEquals(evaluated.checkedActivation.observations, Vector.empty)
    assertEquals(evaluated.effectivePricing, EffectivePricing.Limited(price100))
    assertEquals(evaluated.positionChange, order.intent.positionChange)

  test("activation failure does not suppress independent lot-total and market-role diagnostics"):
    val activation         = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
    val order              = Order.stopMarket(instrument)(Side.Buy, lots10, activation).toOption.get
    val replaySource       = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price99)
    val mismatchedEvidence = replaySource.evidence(price99).toOption.get
    val assumptions        = ScenarioAssumptions.one(order)(
      mismatchedEvidence,
      order.execution.resolution,
      slice(lots9, Rational(100), LiquidityRole.Maker)
    )
    val expected = Vector(
      ScenarioViolation.LotTotal(10, 9),
      ScenarioViolation.Activation(ActivationViolation.FixedEvidenceMismatch),
      ScenarioViolation.MarketSliceNotTaker(0)
    )

    assertEquals(OrderScenario.evaluate(instrument)(assumptions).left.map(_.violations), Left(expected))
    assertEquals(OrderScenario.evaluateFirst(instrument)(assumptions), Left(expected.head))

  test("pricing failure suppresses dependent limit quality but not maker-only validation"):
    val pricing   = PeggedPricing[B, Q](PriceReference.Mark, 3)
    val execution = PricedExecution[D, B, Q, PeggedPricing[B, Q]](
      pricing,
      TimeInForce.Day,
      LiquidityConstraint.MakerOnly,
      DisplayedVisibility
    )
    val order = Order
      .create(instrument)(
        OrderIntent.create(instrument)(Side.Buy, lots10).toOption.get,
        ImmediateActivation[B, Q](),
        execution
      )
      .toOption
      .get
    val replaySource         = PeggedPricing[B, Q](PriceReference.Mark, 2)
    val mismatchedResolution = replaySource.resolution(price99, price100).toOption.get
    val assumptions          = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      mismatchedResolution,
      slice(lots10, Rational(101), LiquidityRole.Taker)
    )
    val expected = Vector(
      ScenarioViolation.Pricing(PricingViolation.PegResolutionMismatch),
      ScenarioViolation.MakerOnlySliceNotMaker(0)
    )

    assertEquals(OrderScenario.evaluate(instrument)(assumptions).left.map(_.violations), Left(expected))

  test("foreign slice identity uses a closed location and suppresses only dependent slice branches"):
    val order       = Order.market(instrument)(Side.Buy, lots10).toOption.get
    val valid       = slice(lots10, Rational(100), LiquidityRole.Maker)
    val foreignId   = fixture.foreignIdentity.identity.id
    val foreign     = LiquiditySlice(foreignId, valid.lots, valid.market, valid.role)
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.resolution,
      foreign
    )
    val expected = ScenarioViolation.Identity(
      ScenarioLocation.Slice(0, ScenarioSliceComponent.Identity),
      instrument.identity.id,
      foreignId
    )

    assertEquals(
      OrderScenario.evaluate(instrument)(assumptions).left.map(_.violations),
      Left(Vector(expected))
    )
end OrderScenarioSuite
