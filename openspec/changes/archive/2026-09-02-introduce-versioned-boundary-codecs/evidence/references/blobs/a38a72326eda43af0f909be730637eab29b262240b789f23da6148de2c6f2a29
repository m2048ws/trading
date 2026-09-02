package trading.codec

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.Rational
import trading.scenario.*

final class ScenarioRecordPropertiesSuite extends ScalaCheckSuite:
  private val fixture    = OrderRecordTestFixture("scenario-properties")
  private val instrument = fixture.instrument

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private val cases =
    for
      lots       <- Gen.chooseNum(1, 10_000)
      entryPrice <- Gen.chooseNum(1, 100_000)
      exitPrice  <- Gen.chooseNum(1, 100_000)
      longFirst  <- Gen.oneOf(true, false)
    yield (BigInt(lots), BigInt(entryPrice), BigInt(exitPrice), longFirst)

  property("exact hypothetical scenario and long/short round-trip records are model-preserving"):
    forAll(cases): (lots, entryPrice, exitPrice, longFirst) =>
      val entrySide = if longFirst then Side.Buy else Side.Sell
      val exitSide  = if longFirst then Side.Sell else Side.Buy
      val entry     = marketScenario(entrySide, lots, entryPrice)
      val exit      = marketScenario(exitSide, lots, exitPrice)

      val entryRecord = OrderScenarioRecord.fromScenario(instrument)(entry)
      val entryWire   = OrderScenarioRecord.encode(entryRecord).toOption.get
      val entryResult = OrderScenarioRecord
        .decodeAndReconstruct(entryWire, instrument, fixture.snapshot)
        .toOption
        .get
      assertEquals(OrderScenarioRecord.fromScenario(instrument)(entryResult), entryRecord)

      val trip       = RoundTripScenario.create(instrument)(entry, exit).toOption.get
      val tripRecord = RoundTripScenarioRecord.fromScenario(instrument)(trip)
      val tripWire   = RoundTripScenarioRecord.encode(tripRecord).toOption.get
      val tripResult = RoundTripScenarioRecord
        .decodeAndReconstruct(tripWire, instrument, fixture.snapshot)
        .toOption
        .get
      assertEquals(RoundTripScenarioRecord.fromScenario(instrument)(tripResult), tripRecord)
      assertEquals(tripResult.heldPosition.coordinate, if longFirst then lots else -lots)

  private def marketScenario(
    side: Side,
    lots: BigInt,
    priceCoordinate: BigInt
  ): OrderScenario[D, B, Q, instrument.MarketState] =
    val checkedLots  = fixture.lots(instrument, lots)
    val checkedPrice = fixture.price(instrument, priceCoordinate)
    val market       = MarketState
      .fromAnchors(instrument)(
        checkedPrice,
        checkedPrice.coefficient * Rational(2),
        Rational(2)
      )
      .toOption
      .get
    val slice = LiquiditySlice
      .create(instrument)(checkedLots, market, LiquidityRole.Taker)
      .toOption
      .get
    val order       = Order.market(instrument)(side, checkedLots).toOption.get
    val assumptions = ScenarioAssumptions
      .one(order)(order.activation.evidence, order.execution.resolution, slice)
      .toOption
      .get
    OrderScenario.evaluate(instrument)(assumptions).toOption.get
  end marketScenario
end ScenarioRecordPropertiesSuite
