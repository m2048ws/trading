package trading.economics.instrument

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole
import trading.reference.AssetId

class EconomicsPropertySuite extends ScalaCheckSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  property("long and short universal price PnL are exact negations"):
    forAll(Gen.choose(1, 100_000)): count =>
      val lots  = instrument.lots(BigInt(count)).toOption.get
      val entry = instrument.market.quoteSettled(fixture.price(instrument, 100)).toOption.get
      val exit  = instrument.market.quoteSettled(fixture.price(instrument, 110)).toOption.get
      val long  = instrument.valuation
        .pricePnl(instrument.positionLots(Side.Buy, lots).toOption.get, entry, exit)
        .toOption
        .get
      val short = instrument.valuation
        .pricePnl(instrument.positionLots(Side.Sell, lots).toOption.get, entry, exit)
        .toOption
        .get

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
      val first       = instrument.scenarios.slice(firstLots, state, LiquidityRole.Maker).toOption.get
      val second      = instrument.scenarios.slice(secondLots, state, LiquidityRole.Taker).toOption.get
      val assumptions = instrument.scenarios.assumptionsMany(order)(
        order.activation.evidence,
        order.execution.pricing.resolution,
        first,
        second
      )
      val scenario = instrument.scenarios.order(order, assumptions).toOption.get
      assertEquals(scenario.assumptions.matchedSlices.toVector.map(_.lots.count.unrefined).sum, BigInt(total))

  property("fee quantization exactly conserves arbitrary rational account contributions"):
    forAll(Gen.choose(-100_000, 100_000), Gen.choose(1, 997)): (numerator, denominator) =>
      val unrounded    = Quantity(fixture.usd.dimension.ref, Rational(numerator, denominator))
      val denomination = instrument.fees
        .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.HalfEven)
        .toOption
        .get
      val fee = denomination.quantize(FeeKind("property"), unrounded)

      assertEquals(fee.amount.coefficient + fee.residual.coefficient, unrounded.coefficient)

  property("exhaustive sizing equals an independently enumerated bounded maximum"):
    forAll(Gen.choose(1, 12), Gen.choose(0, 20)): (capValue, budgetCents) =>
      val cap    = PositiveWhole(capValue).toOption.get
      val budget = Quantity(instrument.roles.settle.dimension.ref, Rational(budgetCents, 100))
      val sized  = instrument.sizing.maxLots(budget, cap, instrument.fees.none): candidate =>
        val entry = completeMarket(Side.Buy, candidate, 100)
        val exit  = completeMarket(Side.Sell, candidate, 90)
        instrument.scenarios.roundTrip(entry, exit)
      val expected = BigInt(1).to(BigInt(capValue)).filter: count =>
        Rational(count, 100).compare(Rational(budgetCents, 100)) <= 0

      assertEquals(sized.map(_.map(_.count.unrefined)), Right(expected.lastOption))

  property("assembly preserves every generated exact payoff coefficient"):
    forAll(Gen.choose(-10_000, 10_000), Gen.choose(1, 997)): (numerator, denominator) =>
      val baseCoefficient  = Rational(numerator, denominator)
      val quoteCoefficient = if baseCoefficient.isZero then Rational.one else Rational.zero
      val definition       = InstrumentDefinition(
        InstrumentIdentity(
          InstrumentId.from(s"property-exact-$numerator-$denominator").toOption.get,
          UnderlyingId.from("property-underlying").toOption.get
        ),
        AssetRoleIds(fixture.btc.id, fixture.usd.id, fixture.contract.id, fixture.usd.id),
        ListingDefinition(fixture.contractLots.identity, fixture.usdPerBtcTicks.identity),
        PayoffDefinition(baseCoefficient, quoteCoefficient)
      )
      val spec = InstrumentAssembler.assemble(definition, fixture.snapshot).toOption.get

      assertEquals(spec.basePerPosition.coefficient, baseCoefficient)
      assertEquals(spec.quotePerPosition.coefficient, quoteCoefficient)
      assertEquals(
        spec.priceGrid.dimension.key,
        DimRef.divide(spec.roles.quote.dimension.ref, spec.roles.base.dimension.ref).key
      )

  property("repeated invalid assembly preserves deterministic stage and role order"):
    forAll(Gen.choose(1, 10_000)): suffix =>
      val missing    = AssetId.from(s"property-missing-$suffix").toOption.get
      val definition = InstrumentDefinition(
        InstrumentIdentity(
          InstrumentId.from(s"property-invalid-$suffix").toOption.get,
          UnderlyingId.from("property-underlying").toOption.get
        ),
        AssetRoleIds(missing, missing, fixture.contract.id, fixture.usd.id),
        ListingDefinition(fixture.contractLots.identity, fixture.usdPerBtcTicks.identity),
        PayoffDefinition(Rational.zero, Rational.zero)
      )
      val first  = InstrumentAssembler.assemble(definition, fixture.snapshot).swap.toOption.get
      val second = InstrumentAssembler.assemble(definition, fixture.snapshot).swap.toOption.get

      assertEquals(first, second)
      assertEquals(InstrumentAssembler.assembleFirst(definition, fixture.snapshot), Left(first.head))

  private def completeMarket(
    side: Side,
    lots: instrument.Lots,
    dollars: BigInt
  ): instrument.OrderScenario =
    fixture.scenario(instrument)(side, lots, fixture.state(instrument, dollars))

end EconomicsPropertySuite
