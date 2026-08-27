package trading.economics

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole

class EconomicsPropertySuite extends ScalaCheckSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  property("long and short universal price PnL are exact negations"):
    forAll(Gen.choose(1, 100_000)): count =>
      val lots  = instrument.lots(BigInt(count)).toOption.get
      val entry = instrument.market.quoteSettled(fixture.price(instrument, 100)).toOption.get
      val exit  = instrument.market.quoteSettled(fixture.price(instrument, 110)).toOption.get
      val long  = instrument.valuation.pricePnl(instrument.positionLots(Side.Buy, lots), entry, exit)
      val short = instrument.valuation.pricePnl(instrument.positionLots(Side.Sell, lots), entry, exit)

      assertEquals(long.coefficient, -short.coefficient)

  property("complete scenario construction conserves every generated positive lot split"):
    forAll(Gen.choose(2, 10_000), Gen.choose(1, 9_999)): (total, proposedFirst) =>
      val firstCount  = 1 + proposedFirst % (total - 1)
      val secondCount = total - firstCount
      val totalLots   = instrument.lots(BigInt(total)).toOption.get
      val firstLots   = instrument.lots(BigInt(firstCount)).toOption.get
      val secondLots  = instrument.lots(BigInt(secondCount)).toOption.get
      val order       = instrument.orders.limit(Side.Buy, totalLots, fixture.price(instrument, 100)).toOption.get
      val state       = instrument.market.quoteSettled(fixture.price(instrument, 100)).toOption.get
      val first       = instrument.scenarios.slice(firstLots, state, LiquidityRole.Maker)
      val second      = instrument.scenarios.slice(secondLots, state, LiquidityRole.Taker)
      val assumptions = instrument.scenarios.assumptions(
        instrument.scenarios.immediate,
        instrument.scenarios.directPricing,
        Vector(first, second)
      )
      val scenario = instrument.scenarios.order(order, assumptions).toOption.get
      assertEquals(scenario.assumptions.matchedSlices.map(_.lots.count.unrefined).sum, BigInt(total))

  property("fee quantization exactly conserves arbitrary rational account contributions"):
    forAll(Gen.choose(-100_000, 100_000), Gen.choose(1, 997)): (numerator, denominator) =>
      val unrounded    = Quantity(fixture.usd.dimension.asDimensionRef, Rational(numerator, denominator))
      val denomination = instrument.fees
        .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.HalfEven)
        .toOption
        .get
      val fee = denomination.quantize(FeeKind("property"), unrounded)

      assertEquals(fee.amount.coefficient + fee.residual.coefficient, unrounded.coefficient)

  property("exhaustive sizing equals an independently enumerated bounded maximum"):
    forAll(Gen.choose(1, 12), Gen.choose(0, 20)): (capValue, budgetCents) =>
      val cap    = PositiveWhole(capValue).toOption.get
      val budget = Quantity(instrument.roles.settle.dimension.asDimensionRef, Rational(budgetCents, 100))
      val sized  = instrument.sizing.maxLots(budget, cap, instrument.fees.none): candidate =>
        val entry = completeMarket(Side.Buy, candidate, 100)
        val exit  = completeMarket(Side.Sell, candidate, 90)
        instrument.scenarios.roundTrip(entry, exit)
      val expected = BigInt(1).to(BigInt(capValue)).filter: count =>
        Rational(count, 100).compare(Rational(budgetCents, 100)) <= 0

      assertEquals(sized.map(_.map(_.count.unrefined)), Right(expected.lastOption))

  private def completeMarket(
    side: Side,
    lots: instrument.Lots,
    dollars: BigInt
  ): instrument.OrderScenario =
    fixture.scenario(instrument)(side, lots, fixture.state(instrument, dollars))

end EconomicsPropertySuite
