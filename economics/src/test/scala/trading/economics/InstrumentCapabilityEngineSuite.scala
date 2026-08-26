package trading.economics

import munit.FunSuite

import trading.quantity.GridId
import trading.quantity.GridKey
import trading.quantity.GridVersion
import trading.quantity.One
import trading.quantity.Rational
import trading.quantity.grid.NotOnGrid
import trading.quantity.grid.QuantizationPolicy

class InstrumentCapabilityEngineSuite extends FunSuite:
  private val fixture = new EconomicsFixtures

  test("price and market plans preserve validation precedence"):
    assertEquals(InstrumentPrices.validateCoordinate(0), Left(InvalidPriceCoordinate(0)))

    val result = InstrumentMarket.checked(
      fixture.btc,
      fixture.usd,
      fixture.eur,
      Rational(100),
      Rational.zero,
      Rational.zero,
      Vector.empty
    )
    assertEquals(
      result,
      Left(InvalidConversion(fixture.btc.id, fixture.eur.id, Rational.zero, "conversion must be positive"))
    )

  test("price engine invokes exact selection and preserves selector failure precedence"):
    val notOnGrid =
      NotOnGrid[One](Rational(5, 4), GridKey(GridId("price-grid"), GridVersion(1)), Rational(1, 2))
    var events = Vector.empty[String]

    val selectionFailure = InstrumentPrices.exact[One, String](() =>
      events :+= "select"
      Left(notOnGrid)
    )(_ =>
      events :+= "coordinate"
      BigInt(0)
    )

    assertEquals(selectionFailure, Left(PriceNotOnGrid(notOnGrid)))
    assertEquals(events, Vector("select"))

    events = Vector.empty
    val coordinateFailure = InstrumentPrices.exact[One, String](() =>
      events :+= "select"
      Right("selected")
    )(_ =>
      events :+= "coordinate"
      BigInt(0)
    )

    assertEquals(coordinateFailure, Left(InvalidPriceCoordinate(0)))
    assertEquals(events, Vector("select", "coordinate"))

  test("price engine invokes policy-driven quantization and observation callbacks"):
    var events    = Vector.empty[String]
    val quantized = InstrumentPrices.quantized(QuantizationPolicy.Floor)(policy =>
      events :+= s"quantize:$policy"
      "selected" -> Rational(1, 4)
    )(_ =>
      events :+= "coordinate"
      BigInt(2)
    )

    assertEquals(quantized, Right("selected" -> Rational(1, 4)))
    assertEquals(events, Vector(s"quantize:${QuantizationPolicy.Floor}", "coordinate"))

    events = Vector.empty
    val observation = InstrumentPrices.observe("selected")(_ =>
      events :+= "coordinate"
      BigInt(2)
    )(_ =>
      events :+= "coefficient"
      Rational(3, 2)
    )

    assertEquals(observation, InstrumentPrices.Observation(BigInt(2), Rational(3, 2)))
    assertEquals(events, Vector("coordinate", "coefficient"))

  test("order planning retains the first universal compatibility failure"):
    val result = InstrumentOrders.checked(
      Side.Buy,
      (),
      (),
      (),
      isMarket = true,
      TimeInForce.GoodTillCancelled,
      LiquidityConstraint.MakerOnly,
      PositionEffect.Unrestricted,
      (),
      VisibilityKind.Displayed,
      None,
      BigInt(1)
    )
    assertEquals(result, Left(InvalidOrder("market orders cannot be maker-only")))

  test("scenario planning checks totals before activation and activation before peg evidence"):
    val activation  = InstrumentScenarios.ActivationView(ActivationKind.Immediate, None, None, None, None)
    val instruction =
      InstrumentScenarios.InstructionView(PriceInstructionKind.Market, None, None, None)
    val order = InstrumentScenarios.OrderView(
      Side.Buy,
      BigInt(1),
      activation,
      instruction,
      LiquidityConstraint.Unrestricted
    )
    val evidence = Some(InstrumentScenarios.EvidenceView(PriceReference.Last, BigInt(1), None))
    val peg      = Some(InstrumentScenarios.PegView(PriceReference.Last, BigInt(1), BigInt(1)))

    assertEquals(
      InstrumentScenarios.order(order, Vector.empty, evidence, peg),
      Left(InvalidScenario("complete scenario requires at least one slice"))
    )
    assertEquals(
      InstrumentScenarios.order(
        order,
        Vector(InstrumentScenarios.SliceView(BigInt(1), BigInt(1), LiquidityRole.Taker)),
        evidence,
        peg
      ),
      Left(InvalidScenario("immediate activation must not carry trigger evidence"))
    )

  test("fee planning checks asset registry before grid registry"):
    val result = InstrumentFees.validateQuantization(
      fixture.btc.id,
      fixture.usd.dimension.key,
      fixture.btc.dimension.key,
      assetSharesSettleRegistry = false,
      fixture.contractLots.key,
      fixture.contract.dimension.key,
      gridSharesAssetRegistry = false
    )
    assertEquals(
      result,
      Left(ForeignRegistry("fee asset", fixture.usd.dimension.key, fixture.btc.dimension.key))
    )

  test("valuation planning assesses entry before exit and stops at the first failure"):
    var assessed = Vector.empty[String]
    val result   = InstrumentValuation.calculatePnl("entry", "exit", Rational.zero)(
      scenario =>
        assessed :+= scenario
        Left(FeeScheduleFailure(scenario))
      ,
      (_, _) => Right(Rational.zero),
      identity
    )

    assertEquals(result, Left(FeeScheduleFailure("entry")))
    assertEquals(assessed, Vector("entry"))

end InstrumentCapabilityEngineSuite
