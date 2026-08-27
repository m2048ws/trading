package trading.economics.instrument

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy

class InstrumentCapabilityEngineSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  test("focused capabilities share the stable runtime identity and operate on real values"):
    val lots        = instrument.lots(2).toOption.get
    val price       = fixture.price(instrument, 100)
    val market      = instrument.market.quoteSettled(price).toOption.get
    val order       = instrument.orders.market(Side.Buy, lots).toOption.get
    val slice       = instrument.scenarios.slice(lots, market, LiquidityRole.Taker).toOption.get
    val assumptions = instrument.scenarios.assumptionsOne(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice
    )
    val scenario = instrument.scenarios.order(order, assumptions).toOption.get

    assertEquals(lots.count.unrefined, BigInt(2))
    assertEquals(price.coefficient, Rational(100))
    assertEquals(scenario.order, order)
    assertEquals(scenario.assumptions, assumptions)

  test("fee denomination validates once and owns subsequent quantization policy"):
    val denomination = instrument.fees
      .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.Ceiling)
      .toOption
      .get
    val fee = denomination.quantize(
      FeeKind("ceiling"),
      Quantity(fixture.usd.dimension.ref, Rational(-1, 200))
    )
    assertEquals(denomination.policy, QuantizationPolicy.Ceiling)
    assertEquals(fee.coordinate, BigInt(0))
    assertEquals(fee.residual.coefficient, Rational(-1, 200))

  test("market state rejects duplicate additional conversions and owns its checked set"):
    val first  = instrument.market.conversion(fixture.token, Rational(2)).toOption.get
    val second = instrument.market.conversion(fixture.token, Rational(3)).toOption.get
    val result = instrument.market.quoteSettled(fixture.price(instrument, 100), Vector(first, second))
    assertEquals(result, Left(DuplicateConversion(fixture.token.id)))

  test("scenario validation reports deterministic first structured failure"):
    val lots        = instrument.lots(2).toOption.get
    val order       = instrument.orders.market(Side.Buy, lots).toOption.get
    val assumptions = instrument.scenarios.assumptionsFromVector(order)(
      order.activation.evidence,
      order.execution.resolution,
      Vector.empty
    )
    assertEquals(
      assumptions,
      Left(InvalidScenarioDiagnostics(ScenarioViolation.EmptySlices, Vector.empty))
    )

end InstrumentCapabilityEngineSuite
