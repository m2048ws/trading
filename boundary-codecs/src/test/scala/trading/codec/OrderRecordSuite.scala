package trading.codec

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.economics.instrument.InvalidLotCount
import trading.economics.instrument.InvalidPriceCoordinate
import trading.order.DisplayedVisibility
import trading.order.FixedActivation
import trading.order.HiddenVisibility
import trading.order.IcebergVisibility
import trading.order.ImmediateActivation
import trading.order.LimitPricing
import trading.order.LiquidityConstraint
import trading.order.MarketExecution
import trading.order.NonRestingTimeInForce
import trading.order.Order
import trading.order.OrderActivation
import trading.order.OrderExecution
import trading.order.OrderIntent
import trading.order.OrderPricing
import trading.order.OrderViolation
import trading.order.PeggedPricing
import trading.order.PositionEffect
import trading.order.PricedExecution
import trading.order.PricedVisibility
import trading.order.PriceReference
import trading.order.Side
import trading.order.TimeInForce
import trading.order.TrailingActivation
import trading.order.TriggerComparison
import trading.quantity.JavaSerializationUnsupported

final class OrderRecordSuite extends FunSuite:
  private val fixture    = OrderRecordTestFixture("order-record")
  private val instrument = fixture.instrument

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private val lots10   = fixture.lots(instrument, 10)
  private val price100 = fixture.price(instrument, 100)

  test("frozen V1 round-trips every valid activation execution pricing visibility side and effect combination"):
    val activations: Vector[OrderActivation[B, Q]] = Vector(
      ImmediateActivation[B, Q](),
      FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100),
      TrailingActivation.create[B, Q](PriceReference.Index, TriggerComparison.AtOrBelow, 3).toOption.get
    )
    val pricings: Vector[OrderPricing[B, Q]] = Vector(
      LimitPricing(price100),
      PeggedPricing[B, Q](PriceReference.Last, -2)
    )
    val visibilities: Vector[PricedVisibility[D]] =
      Vector(DisplayedVisibility, HiddenVisibility, IcebergVisibility(lots10))
    val priced =
      for
        pricing     <- pricings
        timeInForce <- TimeInForce.values.toVector
        liquidity   <- LiquidityConstraint.values.toVector
        visibility  <- visibilities
        if validVisibility(timeInForce, visibility)
      yield PricedExecution[D, B, Q, OrderPricing[B, Q]](
        pricing,
        timeInForce,
        liquidity,
        visibility
      ): OrderExecution[D, B, Q]
    val market = NonRestingTimeInForce.values.toVector.map(value => MarketExecution[D, B, Q](value))
    val executions: Vector[OrderExecution[D, B, Q]] = market ++ priced
    assertEquals(executions.size, 42)

    val records =
      for
        side       <- Side.values.toVector
        effect     <- PositionEffect.values.toVector
        activation <- activations
        execution  <- executions
      yield
        val intent = OrderIntent.create(instrument)(side, lots10, effect).toOption.get
        val order  = Order
          .create[D, B, Q, OrderActivation[B, Q], OrderExecution[D, B, Q]](instrument)(
            intent,
            activation,
            execution
          )
          .toOption
          .get
        val record        = OrderRecord.fromOrder(order)
        val encoded       = OrderRecord.encodeOrder(order).toOption.get
        val parsed        = OrderRecord.parse(encoded).toOption.get
        val reconstructed = OrderRecord.decodeAndReconstruct(encoded, instrument).toOption.get
        assertEquals(parsed, record)
        assertEquals(OrderRecord.fromOrder(reconstructed), record)
        val expectedPosition = if side == Side.Buy then BigInt(10) else BigInt(-10)
        assertEquals(reconstructed.intent.positionChange.coordinate, expectedPosition)
        record
    assertEquals(records.size, 504)
    records.foreach(rejectSerialization)

  test("structural decoding rejects cross-case fields and unknown closed vocabulary"):
    val marketRecord = validMarketRecord
    val marketWire   = OrderRecord.encode(marketRecord).toOption.get
    val crossCase    = marketWire.replace(
      "\"timeInForce\":\"immediateOrCancel\"",
      "\"liquidityConstraint\":\"makerOnly\",\"timeInForce\":\"immediateOrCancel\""
    )
    val crossCaseFailures = OrderRecord.parse(crossCase).left.toOption.get.toVector
    assertEquals(crossCaseFailures.map(_.path.render), Vector("$.payload.execution.liquidityConstraint"))
    assert(crossCaseFailures.head.isInstanceOf[WireDecodeViolation.UnknownField])

    val unknownSide = marketWire.replace("\"side\":\"buy\"", "\"side\":\"twoSided\"")
    OrderRecord.decodeAndReconstruct(unknownSide, instrument) match
      case Left(OrderReconstructionFailure.Codec(violations)) =>
        assertEquals(violations.head.path.render, "$.payload.side")
        assert(violations.head.isInstanceOf[WireDecodeViolation.InvalidValue])
      case other => fail(s"unknown vocabulary must remain structural, got $other")

  test("local refinement failures accumulate deterministically and suppress canonical order construction"):
    val invalid = OrderRecord.V1(
      instrument.identity.id,
      OrderRecord.Side.Buy,
      0,
      OrderRecord.PositionEffect.Unrestricted,
      OrderRecord.Activation.Fixed(
        OrderRecord.PriceReference.Mark,
        OrderRecord.TriggerComparison.AtOrAbove,
        0
      ),
      OrderRecord.Execution.Priced(
        OrderRecord.Pricing.Limit(0),
        OrderRecord.TimeInForce.GoodTillCancelled,
        OrderRecord.LiquidityConstraint.Unrestricted,
        OrderRecord.Visibility.Iceberg(0)
      )
    )
    OrderRecord.decodeAndReconstruct(OrderRecord.encode(invalid).toOption.get, instrument) match
      case Left(OrderReconstructionFailure.Refinement(errors)) =>
        assertEquals(
          errors.failures,
          Vector(
            OrderRefinementFailure.Lots(WirePath.root.field("payload").field("lotCoordinate"), InvalidLotCount(0)),
            OrderRefinementFailure.TriggerPrice(
              WirePath.root.field("payload").field("activation").field("triggerPriceCoordinate"),
              InvalidPriceCoordinate(0)
            ),
            OrderRefinementFailure.LimitPrice(
              WirePath.root.field("payload").field("execution").field("pricing").field("priceCoordinate"),
              InvalidPriceCoordinate(0)
            ),
            OrderRefinementFailure.DisplayedLots(
              WirePath.root.field("payload").field("execution").field("visibility").field(
                "displayedLotsCoordinate"
              ),
              InvalidLotCount(0)
            )
          )
        )
      case other => fail(s"expected complete local refinement failures, got $other")
    end match

    val invalidActivationAndDuration = validMarketRecord.copy(
      activation = OrderRecord.Activation.Trailing(
        OrderRecord.PriceReference.Last,
        OrderRecord.TriggerComparison.AtOrBelow,
        0
      ),
      execution = OrderRecord.Execution.Market(OrderRecord.TimeInForce.Day)
    )
    OrderRecord.reconstruct(invalidActivationAndDuration, instrument) match
      case Left(OrderReconstructionFailure.Refinement(errors)) =>
        assertEquals(
          errors.failures.map(_.getClass.getSimpleName),
          Vector("TrailingOffset", "MarketDuration")
        )
      case other => fail(s"expected trailing and duration refinements, got $other")

  test("canonical order validation retains both independent iceberg violations"):
    val invalid = OrderRecord.V1(
      instrument.identity.id,
      OrderRecord.Side.Sell,
      10,
      OrderRecord.PositionEffect.ReduceOnly,
      OrderRecord.Activation.Immediate,
      OrderRecord.Execution.Priced(
        OrderRecord.Pricing.Limit(100),
        OrderRecord.TimeInForce.ImmediateOrCancel,
        OrderRecord.LiquidityConstraint.Unrestricted,
        OrderRecord.Visibility.Iceberg(11)
      )
    )
    OrderRecord.reconstruct(invalid, instrument) match
      case Left(OrderReconstructionFailure.Validation(violations)) =>
        assertEquals(
          violations.violations,
          Vector(
            OrderViolation.IcebergExceedsOrder(11, 10),
            OrderViolation.NonRestingIceberg
          )
        )
      case other => fail(s"expected canonical aggregate order violations, got $other")

  test("foreign instrument rejection precedes coordinate refinement and batch decoding is atomic and indexed"):
    val foreign = validMarketRecord.copy(
      instrumentId = fixture.foreign.identity.id,
      lotCoordinate = 0,
      activation = OrderRecord.Activation.Fixed(
        OrderRecord.PriceReference.Mark,
        OrderRecord.TriggerComparison.AtOrAbove,
        0
      )
    )
    assertEquals(
      OrderRecord.reconstruct(foreign, instrument),
      Left(
        OrderReconstructionFailure.ForeignInstrument(
          fixture.foreign.identity.id,
          instrument.identity.id
        )
      )
    )

    val validWire      = OrderRecord.encode(validMarketRecord).toOption.get
    val structuralWire = validWire.replace(
      "\"timeInForce\":\"immediateOrCancel\"",
      "\"visibility\":{\"kind\":\"hidden\"},\"timeInForce\":\"immediateOrCancel\""
    )
    val refinementWire = OrderRecord.encode(validMarketRecord.copy(lotCoordinate = 0)).toOption.get
    val validationWire = OrderRecord
      .encode(
        validMarketRecord.copy(
          execution = OrderRecord.Execution.Priced(
            OrderRecord.Pricing.Limit(100),
            OrderRecord.TimeInForce.FillOrKill,
            OrderRecord.LiquidityConstraint.MakerOnly,
            OrderRecord.Visibility.Iceberg(11)
          )
        )
      )
      .toOption
      .get
    val success = OrderRecord.reconstructBatch(Vector(validWire, validWire), instrument).toOption.get
    assertEquals(success.map(OrderRecord.fromOrder), Vector(validMarketRecord, validMarketRecord))

    val failures = OrderRecord
      .reconstructBatch(Vector(validWire, structuralWire, refinementWire, validationWire, validWire), instrument)
      .left
      .toOption
      .get
      .toVector
    assertEquals(failures.map(_.recordIndex), Vector(1, 2, 3))
    assert(failures(0).failure.isInstanceOf[OrderReconstructionFailure.Codec])
    assert(failures(1).failure.isInstanceOf[OrderReconstructionFailure.Refinement])
    assert(failures(2).failure.isInstanceOf[OrderReconstructionFailure.Validation])

    OrderRecord.reconstructBatch(Vector(validWire, "not-json"), instrument, limits(1)) match
      case Left(errors) =>
        assertEquals(errors.toVector.size, 1)
        errors.head.failure match
          case OrderReconstructionFailure.Codec(violations) =>
            violations.head match
              case WireDecodeViolation.Limit(limit) =>
                assertEquals(limit.limit, DecodeLimit.BatchRecords)
                assertEquals(limit.actual, 2L)
              case other => fail(s"expected one batch limit, got $other")
          case other => fail(s"expected codec-stage batch limit, got $other")
      case other => fail(s"oversized batch must fail before record work, got $other")

  test("frozen schema and records omit derived authority lifecycle scenario and execution-fact fields"):
    val record  = validMarketRecord
    val encoded = OrderRecord.encode(record).toOption.get
    val schema  = OrderRecord.schema().toOption.get
    assertEquals(
      classOf[OrderRecord.V1].getDeclaredFields.map(_.getName).toSet,
      Set("instrumentId", "side", "lotCoordinate", "positionEffect", "activation", "execution")
    )
    Vector(
      "instrumentId",
      "side",
      "lotCoordinate",
      "positionEffect",
      "activation",
      "execution",
      "triggerPriceCoordinate",
      "offsetTicks",
      "priceCoordinate",
      "displayedLotsCoordinate"
    ).foreach(field => assert(schema.contains(field)))
    Vector(
      "positionChange",
      "componentInstrumentId",
      "scenario",
      "venue",
      "lifecycle",
      "fill",
      "reportedFee",
      "account",
      "catalogRevision",
      "lineage",
      "snapshot",
      "handle"
    ).foreach: forbidden =>
      assert(!schema.contains(forbidden), forbidden)
      assert(!encoded.contains(s"\"$forbidden\""), forbidden)
    rejectSerialization(record)
    rejectSerialization(record.side)
    rejectSerialization(record.positionEffect)
    rejectSerialization(record.activation)
    rejectSerialization(record.execution)
    rejectSerialization(OrderRecord.Pricing.Limit(100))
    rejectSerialization(OrderRecord.Visibility.Iceberg(5))

  private def validVisibility(timeInForce: TimeInForce, visibility: PricedVisibility[D]): Boolean =
    visibility match
      case _: IcebergVisibility[?] =>
        timeInForce != TimeInForce.ImmediateOrCancel && timeInForce != TimeInForce.FillOrKill
      case _ => true

  private def validMarketRecord: OrderRecord.V1 =
    OrderRecord.V1(
      instrument.identity.id,
      OrderRecord.Side.Buy,
      10,
      OrderRecord.PositionEffect.Unrestricted,
      OrderRecord.Activation.Immediate,
      OrderRecord.Execution.Market(OrderRecord.TimeInForce.ImmediateOrCancel)
    )

  private def rejectSerialization(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  private def limits(maxBatchRecords: Int): DecodeLimits =
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
        maxScenarioSlices = 10_000,
        maxMarketConversions = 1_024
      )
      .toOption
      .get
end OrderRecordSuite
