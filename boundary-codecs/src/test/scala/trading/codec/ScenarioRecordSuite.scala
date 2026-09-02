package trading.codec

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.reference.*
import trading.scenario.*

final class ScenarioRecordSuite extends FunSuite:
  private val fixture    = OrderRecordTestFixture("scenario-record")
  private val instrument = fixture.instrument

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private val lots10   = fixture.lots(instrument, 10)
  private val price100 = fixture.price(instrument, 100)
  private val price101 = fixture.price(instrument, 101)
  private val price102 = fixture.price(instrument, 102)

  test("V1 round-trips every associated evidence shape and retains ordered slices and conversions"):
    val immediate =
      val order = Order.limit(instrument)(Side.Buy, lots10, price102).toOption.get
      scenario(order)(
        order.activation.evidence,
        order.execution.pricing.resolution,
        slice(4, 100, LiquidityRole.Maker, Vector(fixture.token -> Rational(3), fixture.rebate -> Rational(5))),
        slice(6, 101, LiquidityRole.Taker, Vector(fixture.rebate -> Rational(7), fixture.token -> Rational(11)))
      )

    val fixed =
      val activation = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
      val execution  = MarketExecution[D, B, Q](NonRestingTimeInForce.ImmediateOrCancel)
      val order      = Order
        .create(instrument)(OrderIntent.create(instrument)(Side.Buy, lots10).toOption.get, activation, execution)
        .toOption
        .get
      scenario(order)(
        activation.evidence(price101).toOption.get,
        execution.resolution,
        slice(10, 101, LiquidityRole.Taker)
      )

    val trailingPegged =
      val activation = TrailingActivation
        .create[B, Q](PriceReference.Index, TriggerComparison.AtOrBelow, 3)
        .toOption
        .get
      val pricing   = PeggedPricing[B, Q](PriceReference.Mark, 2)
      val execution = PricedExecution[D, B, Q, PeggedPricing[B, Q]](
        pricing,
        TimeInForce.Day,
        LiquidityConstraint.Unrestricted,
        DisplayedVisibility
      )
      val order = Order
        .create(instrument)(OrderIntent.create(instrument)(Side.Buy, lots10).toOption.get, activation, execution)
        .toOption
        .get
      scenario(order)(
        activation.evidence(price100, fixture.price(instrument, 97)).toOption.get,
        pricing.resolution(price100, price102).toOption.get,
        slice(10, 101, LiquidityRole.Maker)
      )
    end trailingPegged

    val scenarios                = Vector(immediate, fixed, trailingPegged)
    val expectedActivationShapes = Vector(
      ScenarioEvidenceShape.ImmediateActivation,
      ScenarioEvidenceShape.FixedActivation,
      ScenarioEvidenceShape.TrailingActivation
    )
    val expectedPricingShapes = Vector(
      ScenarioEvidenceShape.DirectPricing,
      ScenarioEvidenceShape.DirectPricing,
      ScenarioEvidenceShape.PeggedPricing
    )

    scenarios.zipWithIndex.foreach: (value, index) =>
      val record        = OrderScenarioRecord.fromScenario(instrument)(value)
      val wire          = OrderScenarioRecord.encode(record).toOption.get
      val parsed        = OrderScenarioRecord.parse(wire).toOption.get
      val reconstructed = OrderScenarioRecord.decodeAndReconstruct(wire, instrument, fixture.snapshot).toOption.get
      assertEquals(parsed, record)
      assertEquals(OrderScenarioRecord.fromScenario(instrument)(reconstructed), record)
      assertEquals(activationShape(record.activationEvidence), expectedActivationShapes(index))
      assertEquals(pricingShape(record.pricingResolution), expectedPricingShapes(index))
      assertEquals(OrderScenarioRecord.encode(parsed), Right(wire))
      rejectSerialization(record)

    val retained = OrderScenarioRecord.fromScenario(instrument)(immediate)
    assertEquals(retained.slices.map(_.lotCoordinate), Vector(BigInt(4), BigInt(6)))
    assertEquals(
      retained.slices.map(_.market.additionalConversions.map(_.sourceAssetId)),
      Vector(Vector(fixture.token.id, fixture.rebate.id), Vector(fixture.rebate.id, fixture.token.id))
    )
    assertEquals(
      retained.slices.map(_.market.additionalConversions.map(_.sourceToSettle)),
      Vector(Vector(Rational(3), Rational(5)), Vector(Rational(7), Rational(11)))
    )

  test("associated evidence replay rejects shape and same-shape semantic mismatches with stable paths"):
    val activation = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
    val execution  = MarketExecution[D, B, Q](NonRestingTimeInForce.ImmediateOrCancel)
    val order      = Order
      .create(instrument)(OrderIntent.create(instrument)(Side.Buy, lots10).toOption.get, activation, execution)
      .toOption
      .get
    val valid = OrderScenarioRecord.fromScenario(instrument)(
      scenario(order)(
        activation.evidence(price101).toOption.get,
        execution.resolution,
        slice(10, 101, LiquidityRole.Taker)
      )
    )

    preparation(valid.copy(activationEvidence = OrderScenarioRecord.ActivationEvidence.Immediate)) match
      case ScenarioPreparationFailure.EvidenceShape(path, expected, supplied) =>
        assertEquals(path.render, "$.payload.activationEvidence")
        assertEquals(expected, ScenarioEvidenceShape.FixedActivation)
        assertEquals(supplied, ScenarioEvidenceShape.ImmediateActivation)
      case other => fail(s"expected associated activation shape failure, got $other")

    preparation(valid.copy(
      activationEvidence = OrderScenarioRecord.ActivationEvidence.Fixed(99)
    )) match
      case ScenarioPreparationFailure.Activation(path, ActivationViolation.FixedTriggerUnsatisfied) =>
        assertEquals(path.render, "$.payload.activationEvidence")
      case other => fail(s"expected fixed-trigger replay failure, got $other")

    val pegged = trailingPeggedRecord
    preparation(pegged.copy(
      pricingResolution = OrderScenarioRecord.PricingResolution.Pegged(100, 103)
    )) match
      case ScenarioPreparationFailure.Pricing(path, PricingViolation.PegOffsetMismatch(2, 3)) =>
        assertEquals(path.render, "$.payload.pricingResolution")
      case other => fail(s"expected peg replay failure, got $other")

  test("slice reconstruction delegates catalog conversions market coherence and non-empty structure"):
    val valid  = simpleMarketRecord
    val market = valid.slices.head.market

    val incoherent = valid.copy(slices = valid.slices.updated(0,
      valid.slices.head.copy(market = market.copy(baseToSettle = market.baseToSettle + Rational.one))))
    preparation(incoherent) match
      case ScenarioPreparationFailure.Market(path, violations) =>
        assertEquals(path.render, "$.payload.slices[0].market")
        assert(violations.violations.exists(_.isInstanceOf[MarketStateViolation.IncoherentAnchors]))
      case other => fail(s"expected market-anchor failure, got $other")

    preparation(valid.copy(slices = Vector.empty)) match
      case ScenarioPreparationFailure.MatchedSlices(path, ScenarioViolation.EmptySlices) =>
        assertEquals(path.render, "$.payload.slices")
      case other => fail(s"expected non-empty matched-slices failure, got $other")

    val unknown = AssetId.from("missing-conversion-source").toOption.get
    val missing = valid.copy(slices = valid.slices.updated(0,
      valid.slices.head.copy(market = market.copy(
        additionalConversions = Vector(OrderScenarioRecord.AdditionalConversion(unknown, Rational.one))
      ))))
    preparation(missing) match
      case ScenarioPreparationFailure.Catalog(path, source, UnknownAsset(id)) =>
        assertEquals(path.render, "$.payload.slices[0].market.additionalConversions[0].sourceAssetId")
        assertEquals(source, unknown)
        assertEquals(id, unknown)
      case other => fail(s"expected catalog lookup failure, got $other")

    val foreignSnapshot =
      val definition = AssetDefinition(fixture.token.id, fixture.token.dimension.key.powers.head._1)
      CatalogModel
        .commit(
          CatalogRoot.create().initialState,
          CatalogBatch.of(CatalogCommand.RegisterAsset(definition))
        )
        .toOption
        .get
        .state
        .snapshot
    val crossLineage = valid.copy(slices = valid.slices.updated(0,
      valid.slices.head.copy(market = market.copy(
        additionalConversions = Vector(
          OrderScenarioRecord.AdditionalConversion(fixture.token.id, Rational(2))
        )
      ))))
    OrderScenarioRecord.reconstruct(crossLineage, instrument, foreignSnapshot) match
      case Left(OrderScenarioReconstructionFailure.Preparation(errors)) =>
        errors.head match
          case ScenarioPreparationFailure.Conversion(path, MarketStateViolation.ReferenceData(_, _)) =>
            assertEquals(path.render, "$.payload.slices[0].market.additionalConversions[0].sourceToSettle")
          case other => fail(s"expected cross-lineage conversion failure, got $other")
      case other => fail(s"cross-lineage source must fail, got $other")

  test("canonical scenario evaluation retains lot limit and liquidity violations after reconstruction"):
    val valid          = simpleLimitRecord
    val mismatchedLots = valid.copy(slices = valid.slices.map(_.copy(lotCoordinate = 9)))
    validation(mismatchedLots) match
      case Vector(ScenarioViolation.LotTotal(10, 9)) => ()
      case other                                     => fail(s"expected lot-total failure, got $other")

    val worseMarket = valid.slices.head.market.copy(
      priceCoordinate = 202,
      baseToSettle = Rational(202),
      quoteToSettle = Rational(2)
    )
    val worse = valid.copy(slices = valid.slices.updated(0, valid.slices.head.copy(market = worseMarket)))
    assertEquals(validation(worse), Vector(ScenarioViolation.SliceWorseThanLimit(0)))

    val marketMaker = simpleMarketRecord.copy(
      slices = simpleMarketRecord.slices.map(_.copy(liquidity = OrderScenarioRecord.Liquidity.Maker))
    )
    assertEquals(validation(marketMaker), Vector(ScenarioViolation.MarketSliceNotTaker(0)))

    val makerOnlyOrder = Order.limit(
      instrument
    )(
      Side.Buy,
      lots10,
      price102,
      liquidityConstraint = LiquidityConstraint.MakerOnly
    ).toOption.get
    val makerOnly = OrderScenarioRecord.fromScenario(instrument)(
      scenario(makerOnlyOrder)(
        makerOnlyOrder.activation.evidence,
        makerOnlyOrder.execution.pricing.resolution,
        slice(10, 101, LiquidityRole.Maker)
      )
    )
    val taker = makerOnly.copy(
      slices = makerOnly.slices.map(_.copy(liquidity = OrderScenarioRecord.Liquidity.Taker))
    )
    assertEquals(validation(taker), Vector(ScenarioViolation.MakerOnlySliceNotMaker(0)))

  test("round-trip V1 is entry times exit and independently accumulates leg failures before flat proof"):
    val entry   = simpleMarketScenario(Side.Buy, 100)
    val exit    = simpleMarketScenario(Side.Sell, 102)
    val trip    = RoundTripScenario.create(instrument)(entry, exit).toOption.get
    val record  = RoundTripScenarioRecord.fromScenario(instrument)(trip)
    val wire    = RoundTripScenarioRecord.encode(record).toOption.get
    val decoded = RoundTripScenarioRecord
      .decodeAndReconstruct(wire, instrument, fixture.snapshot)
      .toOption
      .get
    assertEquals(RoundTripScenarioRecord.fromScenario(instrument)(decoded), record)
    assertEquals(decoded.heldPosition.coordinate, BigInt(10))

    val invalid = record.copy(
      entry = record.entry.copy(slices = Vector.empty),
      exit = record.exit.copy(slices = Vector.empty)
    )
    RoundTripScenarioRecord.reconstruct(invalid, instrument, fixture.snapshot) match
      case Left(RoundTripScenarioReconstructionFailure.Legs(errors)) =>
        assertEquals(errors.failures.map(_.leg), Vector(RoundTripLeg.Entry, RoundTripLeg.Exit))
      case other => fail(s"expected independent entry/exit failures, got $other")

    val notFlat = record.copy(exit = record.exit.copy(
      order = record.exit.order.copy(lotCoordinate = 9),
      slices = record.exit.slices.map(_.copy(lotCoordinate = 9))
    ))
    RoundTripScenarioRecord.reconstruct(notFlat, instrument, fixture.snapshot) match
      case Left(RoundTripScenarioReconstructionFailure.Validation(
          RoundTripViolation.PositionNotFlat(10, -9)
        )) => ()
      case other => fail(s"expected canonical flat-position failure, got $other")

  test("named scenario limits and encoded scenario batches are deterministic atomic and snapshot-coherent"):
    val order = Order.market(instrument)(Side.Buy, lots10).toOption.get
    val multi = OrderScenarioRecord.fromScenario(instrument)(
      scenario(order)(
        order.activation.evidence,
        order.execution.resolution,
        slice(4, 100, LiquidityRole.Taker, Vector(fixture.token -> Rational(3), fixture.rebate -> Rational(5))),
        slice(6, 101, LiquidityRole.Taker)
      )
    )
    val multiWire = OrderScenarioRecord.encode(multi).toOption.get

    OrderScenarioRecord.parse(multiWire, limits(maxScenarioSlices = 1)).left.toOption.get.head match
      case WireDecodeViolation.Limit(value) =>
        assertEquals(value.limit, DecodeLimit.ScenarioSlices)
        assertEquals(value.path.render, "$.payload.slices")
      case other => fail(s"expected named scenario-slice limit, got $other")

    OrderScenarioRecord.parse(multiWire, limits(maxMarketConversions = 1)).left.toOption.get.head match
      case WireDecodeViolation.Limit(value) =>
        assertEquals(value.limit, DecodeLimit.MarketConversions)
        assertEquals(value.path.render, "$.payload.slices[0].market.additionalConversions")
      case other => fail(s"expected named market-conversion limit, got $other")

    val validWire   = OrderScenarioRecord.encode(simpleMarketRecord).toOption.get
    val invalidWire = OrderScenarioRecord.encode(simpleMarketRecord.copy(slices = Vector.empty)).toOption.get
    val successful  = OrderScenarioRecord
      .reconstructBatch(Vector(validWire, validWire), instrument, fixture.snapshot)
      .toOption
      .get
    assertEquals(successful.map(OrderScenarioRecord.fromScenario(instrument)),
      Vector(simpleMarketRecord, simpleMarketRecord))

    val failures = OrderScenarioRecord
      .reconstructBatch(Vector(validWire, "not-json", invalidWire, validWire), instrument, fixture.snapshot)
      .left
      .toOption
      .get
      .toVector
    assertEquals(failures.map(_.recordIndex), Vector(1, 2))
    assert(failures.head.failure.isInstanceOf[OrderScenarioReconstructionFailure.Codec])
    assert(failures.last.failure.isInstanceOf[OrderScenarioReconstructionFailure.Preparation])

    OrderScenarioRecord
      .reconstructBatch(Vector(validWire, "not-json"), instrument, fixture.snapshot, limits(maxBatchRecords = 1)) match
      case Left(errors) =>
        assertEquals(errors.toVector.size, 1)
        errors.head.failure match
          case OrderScenarioReconstructionFailure.Codec(violations) =>
            violations.head match
              case WireDecodeViolation.Limit(value) =>
                assertEquals(value.limit, DecodeLimit.BatchRecords)
                assertEquals(value.actual, 2L)
              case other => fail(s"expected one pre-record batch limit, got $other")
          case other => fail(s"expected codec-stage batch limit, got $other")
      case other => fail(s"oversized scenario batch must fail before record work, got $other")

    val trip = RoundTripScenario
      .create(instrument)(simpleMarketScenario(Side.Buy, 100), simpleMarketScenario(Side.Sell, 102))
      .toOption
      .get
    val tripWire     = RoundTripScenarioRecord.encodeScenario(instrument)(trip).toOption.get
    val tripFailures = RoundTripScenarioRecord
      .reconstructBatch(Vector(tripWire, "not-json", tripWire), instrument, fixture.snapshot)
      .left
      .toOption
      .get
      .toVector
    assertEquals(tripFailures.map(_.recordIndex), Vector(1))
    assert(tripFailures.head.failure.isInstanceOf[RoundTripScenarioReconstructionFailure.Codec])
    RoundTripScenarioRecord
      .reconstructBatch(Vector(tripWire, "not-json"), instrument, fixture.snapshot, limits(maxBatchRecords = 1)) match
      case Left(errors) =>
        assertEquals(errors.toVector.size, 1)
        errors.head.failure match
          case RoundTripScenarioReconstructionFailure.Codec(violations) =>
            violations.head match
              case WireDecodeViolation.Limit(value) => assertEquals(value.limit, DecodeLimit.BatchRecords)
              case other                            => fail(s"expected round-trip batch limit, got $other")
          case other => fail(s"expected round-trip codec-stage batch limit, got $other")
      case other => fail(s"oversized round-trip batch must fail before record work, got $other")

  test("schemas encode hypothetical assumptions only and omit execution fee PnL and lifecycle facts"):
    val scenarioSchema = OrderScenarioRecord.schema().toOption.get
    val tripSchema     = RoundTripScenarioRecord.schema().toOption.get
    val wire           = OrderScenarioRecord.encode(simpleMarketRecord).toOption.get
    val golden         =
      """{"payload":{"activationEvidence":{"kind":"immediate"},"order":{"activation":{"kind":"immediate"},"execution":{"kind":"market","timeInForce":"immediateOrCancel"},"instrumentId":"scenario-record-primary","lotCoordinate":"10","positionEffect":"unrestricted","side":"buy"},"pricingResolution":{"kind":"direct"},"slices":[{"liquidity":"taker","lotCoordinate":"10","market":{"additionalConversions":[],"baseToSettle":{"denominator":"1","numerator":"100"},"priceCoordinate":"100","quoteToSettle":{"denominator":"1","numerator":"2"}}}]},"recordType":"trading.order-scenario","schemaVersion":1}"""
    assertEquals(wire, golden)

    Vector(
      "order",
      "activationEvidence",
      "pricingResolution",
      "slices",
      "lotCoordinate",
      "liquidity",
      "priceCoordinate",
      "baseToSettle",
      "quoteToSettle",
      "additionalConversions",
      "sourceAssetId",
      "sourceToSettle"
    ).foreach(field => assert(scenarioSchema.contains(field), field))
    Vector("entry", "exit").foreach(field => assert(tripSchema.contains(field), field))
    Vector(
      "actualExecution",
      "fillId",
      "venue",
      "feePolicy",
      "fee",
      "pricePnl",
      "netPnl",
      "heldPosition",
      "lifecycle",
      "targetAssetId",
      "catalogRevision",
      "snapshot"
    ).foreach: forbidden =>
      assert(!scenarioSchema.contains(forbidden), forbidden)
      assert(!tripSchema.contains(forbidden), forbidden)
      assert(!wire.contains(s"\"$forbidden\""), forbidden)
    assertEquals(simpleMarketRecord.order, OrderRecord.fromOrder(simpleMarketScenario(Side.Buy, 100).order))

  private def scenario[A <: OrderActivation[B, Q], E <: OrderExecution[D, B, Q]](
    order: Order.Aux[D, B, Q, A, E]
  )(
    activation: order.activation.Evidence,
    pricing: order.execution.Resolution,
    slices: LiquiditySlice[instrument.Lots, instrument.MarketState]*
  ): OrderScenario[D, B, Q, instrument.MarketState] =
    val assumptions = ScenarioAssumptions
      .fromVector(order)(activation, pricing, slices.toVector)
      .toOption
      .get
    OrderScenario.evaluate(instrument)(assumptions).toOption.get

  private def slice(
    lots: BigInt,
    price: BigInt,
    role: LiquidityRole,
    additional: Vector[(Asset, Rational)] = Vector.empty
  ): LiquiditySlice[instrument.Lots, instrument.MarketState] =
    val conversions = additional.map: (source, rate) =>
      SettlementConversion.exact(instrument)(source)(rate).toOption.get
    val checkedPrice = fixture.price(instrument, price)
    val quoteRate    = Rational(2)
    val market       = MarketState
      .fromAnchors(instrument)(
        checkedPrice,
        checkedPrice.coefficient * quoteRate,
        quoteRate,
        conversions
      )
      .toOption
      .get
    LiquiditySlice
      .create(instrument)(fixture.lots(instrument, lots), market, role)
      .toOption
      .get
  end slice

  private def simpleMarketScenario(side: Side, price: BigInt): OrderScenario[D, B, Q, instrument.MarketState] =
    val order = Order.market(instrument)(side, lots10).toOption.get
    scenario(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice(10, price, LiquidityRole.Taker)
    )

  private def simpleMarketRecord: OrderScenarioRecord.V1 =
    OrderScenarioRecord.fromScenario(instrument)(simpleMarketScenario(Side.Buy, 100))

  private def simpleLimitRecord: OrderScenarioRecord.V1 =
    val order = Order.limit(instrument)(Side.Buy, lots10, price100).toOption.get
    OrderScenarioRecord.fromScenario(instrument)(
      scenario(order)(
        order.activation.evidence,
        order.execution.pricing.resolution,
        slice(10, 100, LiquidityRole.Maker)
      )
    )

  private def trailingPeggedRecord: OrderScenarioRecord.V1 =
    val activation = TrailingActivation
      .create[B, Q](PriceReference.Index, TriggerComparison.AtOrBelow, 3)
      .toOption
      .get
    val pricing   = PeggedPricing[B, Q](PriceReference.Mark, 2)
    val execution = PricedExecution[D, B, Q, PeggedPricing[B, Q]](
      pricing,
      TimeInForce.Day,
      LiquidityConstraint.Unrestricted,
      DisplayedVisibility
    )
    val order = Order
      .create(instrument)(OrderIntent.create(instrument)(Side.Buy, lots10).toOption.get, activation, execution)
      .toOption
      .get
    OrderScenarioRecord.fromScenario(instrument)(
      scenario(order)(
        activation.evidence(price100, fixture.price(instrument, 97)).toOption.get,
        pricing.resolution(price100, price102).toOption.get,
        slice(10, 101, LiquidityRole.Maker)
      )
    )
  end trailingPeggedRecord

  private def preparation(record: OrderScenarioRecord.V1): ScenarioPreparationFailure =
    OrderScenarioRecord.reconstruct(record, instrument, fixture.snapshot) match
      case Left(OrderScenarioReconstructionFailure.Preparation(errors)) => errors.head
      case other => fail(s"expected scenario preparation failure, got $other")

  private def validation(record: OrderScenarioRecord.V1): Vector[ScenarioViolation] =
    OrderScenarioRecord.reconstruct(record, instrument, fixture.snapshot) match
      case Left(OrderScenarioReconstructionFailure.Validation(errors)) => errors.violations
      case other => fail(s"expected scenario validation failure, got $other")

  private def activationShape(value: OrderScenarioRecord.ActivationEvidence): ScenarioEvidenceShape =
    value match
      case OrderScenarioRecord.ActivationEvidence.Immediate   => ScenarioEvidenceShape.ImmediateActivation
      case _: OrderScenarioRecord.ActivationEvidence.Fixed    => ScenarioEvidenceShape.FixedActivation
      case _: OrderScenarioRecord.ActivationEvidence.Trailing => ScenarioEvidenceShape.TrailingActivation

  private def pricingShape(value: OrderScenarioRecord.PricingResolution): ScenarioEvidenceShape =
    value match
      case OrderScenarioRecord.PricingResolution.Direct    => ScenarioEvidenceShape.DirectPricing
      case _: OrderScenarioRecord.PricingResolution.Pegged => ScenarioEvidenceShape.PeggedPricing

  private def limits(
    maxBatchRecords: Int = 10_000,
    maxScenarioSlices: Int = 10_000,
    maxMarketConversions: Int = 1_024
  ): DecodeLimits =
    DecodeLimits
      .create(
        maxPayloadCharacters = 1_000_000,
        maxPayloadUtf8Bytes = 4_000_000,
        maxNestingDepth = 32,
        maxBatchRecords = maxBatchRecords,
        maxObjectMembers = 128,
        maxArrayEntries = 10_000,
        maxStringCharacters = 4_096,
        maxIntegerDigits = 4_096,
        maxDimensionFactors = 256,
        maxCatalogCommands = 10_000,
        maxScenarioSlices = maxScenarioSlices,
        maxMarketConversions = maxMarketConversions
      )
      .toOption
      .get

  private def rejectSerialization(value: AnyRef): Unit =
    val output = ByteArrayOutputStream()
    val stream = ObjectOutputStream(output)
    try
      val _ = intercept[NotSerializableException](stream.writeObject(value))
    finally stream.close()
end ScenarioRecordSuite
