package trading.scenario

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*

final class ScenarioPropertiesSuite extends ScalaCheckSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private def scenario(
    side: Side,
    lots: instrument.Lots,
    price: Rational
  ): OrderScenario[D, B, Q, instrument.MarketState] =
    val order = Order.market(instrument)(side, lots).toOption.get
    val slice = LiquiditySlice
      .create(instrument)(
        lots,
        fixture.quoteState(instrument, price),
        LiquidityRole.Taker
      )
      .toOption
      .get
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice
    )
    OrderScenario.evaluate(instrument)(assumptions).toOption.get

  property("equal opposite retained positions close exactly while unequal positions retain signed failure evidence"):
    forAll { (raw: Int, buyEntry: Boolean) =>
      val count     = BigInt(raw).abs + 1
      val entrySide = if buyEntry then Side.Buy else Side.Sell
      val exitSide  = if buyEntry then Side.Sell else Side.Buy
      val entryLots = Lots.fromCount(instrument)(count).toOption.get
      val exitLots  = Lots.fromCount(instrument)(count).toOption.get
      val entry     = scenario(entrySide, entryLots, Rational(99))
      val exit      = scenario(exitSide, exitLots, Rational(100))
      val closed    = RoundTripScenario.create(instrument)(entry, exit).toOption.get

      val unequalLots     = Lots.fromCount(instrument)(count + 1).toOption.get
      val unequalExit     = scenario(exitSide, unequalLots, Rational(100))
      val expectedFailure = RoundTripViolation.PositionNotFlat(
        entrySide.sign * count,
        exitSide.sign * (count + 1)
      )

      closed.heldPosition == entry.positionChange &&
      closed.entry == entry &&
      closed.exit == exit &&
      RoundTripScenario.create(instrument)(entry, unequalExit) == Left(expectedFailure)
    }
end ScenarioPropertiesSuite
