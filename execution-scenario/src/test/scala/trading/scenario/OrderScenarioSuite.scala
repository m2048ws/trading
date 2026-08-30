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

  private def marketScenario(
    side: Side,
    lots: instrument.Lots,
    price: Rational
  ): OrderScenario[D, B, Q, instrument.MarketState] =
    val order       = Order.market(instrument)(side, lots).toOption.get
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice(lots, price, LiquidityRole.Taker)
    )
    OrderScenario.evaluate(instrument)(assumptions).toOption.get

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

  test("foreign slices use closed locations and suppress only dependent slice branches"):
    val order             = Order.market(instrument)(Side.Buy, lots10).toOption.get
    val foreignInstrument = fixture.foreignIdentity
    val foreignLots       = fixture.lots(foreignInstrument, 10)
    val foreignMarket     = fixture.quoteState(foreignInstrument, Rational(100))
    val foreignId         = foreignInstrument.identity.id
    val foreign           = LiquiditySlice
      .create(foreignInstrument)(foreignLots, foreignMarket, LiquidityRole.Maker)
      .toOption
      .get
      .asInstanceOf[LiquiditySlice[instrument.Lots, instrument.MarketState]]
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.resolution,
      foreign
    )
    val expected = Vector(
      ScenarioSliceComponent.Identity,
      ScenarioSliceComponent.Lots,
      ScenarioSliceComponent.Market,
      ScenarioSliceComponent.Price
    ).map(component =>
      ScenarioViolation.Identity(
        ScenarioLocation.Slice(0, component),
        instrument.identity.id,
        foreignId
      )
    )

    assertEquals(
      OrderScenario.evaluate(instrument)(assumptions).left.map(_.violations),
      Left(expected)
    )

  test("round trips preserve complete long and short legs and the entry's retained position"):
    val longEntry = marketScenario(Side.Buy, lots10, Rational(99))
    val longExit  = marketScenario(Side.Sell, lots10, Rational(100))
    val longTrip  = RoundTripScenario.create(instrument)(longEntry, longExit).toOption.get

    assertEquals(longTrip.entry, longEntry)
    assertEquals(longTrip.exit, longExit)
    assertEquals(longTrip.entry.assumptions, longEntry.assumptions)
    assertEquals(longTrip.exit.assumptions, longExit.assumptions)
    assertEquals(longTrip.heldPosition, longEntry.positionChange)
    assertEquals(longTrip.heldPosition.coordinate, BigInt(10))

    val shortEntry = marketScenario(Side.Sell, lots10, Rational(100))
    val shortExit  = marketScenario(Side.Buy, lots10, Rational(99))
    val shortTrip  = RoundTripScenario.create(instrument)(shortEntry, shortExit).toOption.get

    assertEquals(shortTrip.entry, shortEntry)
    assertEquals(shortTrip.exit, shortExit)
    assertEquals(shortTrip.heldPosition, shortEntry.positionChange)
    assertEquals(shortTrip.heldPosition.coordinate, BigInt(-10))

  test("round trips reject non-flat checked position combinations with both signed coordinates"):
    val entry = marketScenario(Side.Buy, lots10, Rational(99))
    val exit  = marketScenario(Side.Sell, lots9, Rational(100))

    assertEquals(
      RoundTripScenario.create(instrument)(entry, exit),
      Left(RoundTripViolation.PositionNotFlat(BigInt(10), BigInt(-9)))
    )

  test("round trips reject foreign legs with typed components"):
    val foreign      = fixture.foreignIdentity
    val foreignLots  = fixture.lots(foreign, 10)
    val foreignOrder = Order.market(foreign)(Side.Buy, foreignLots).toOption.get
    val foreignSlice = LiquiditySlice
      .create(foreign)(
        foreignLots,
        fixture.quoteState(foreign, Rational(99)),
        LiquidityRole.Taker
      )
      .toOption
      .get
    val foreignAssumptions = ScenarioAssumptions.one(foreignOrder)(
      foreignOrder.activation.evidence,
      foreignOrder.execution.resolution,
      foreignSlice
    )
    val foreignEntry = OrderScenario
      .evaluate(foreign)(foreignAssumptions)
      .toOption
      .get
      .asInstanceOf[OrderScenario[D, B, Q, instrument.MarketState]]
    val exit = marketScenario(Side.Sell, lots10, Rational(100))

    assertEquals(
      RoundTripScenario.create(instrument)(foreignEntry, exit),
      Left(
        RoundTripViolation.InstrumentMismatch(
          RoundTripComponent.Entry,
          instrument.identity.id,
          foreign.identity.id
        )
      )
    )

end OrderScenarioSuite
