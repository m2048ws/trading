package trading.codec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

import munit.FunSuite

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.Rational
import trading.reference.*
import trading.scenario.*

final class ScenarioRecordScopeSuite extends FunSuite:
  private val fixture    = OrderRecordTestFixture("scenario-scope")
  private val instrument = fixture.instrument
  private val encoder    = ScenarioRecord.encoder(instrument)
  private val decoder    = ScenarioRecord.decoder(instrument, fixture.snapshot)

  private type D = instrument.PositionD
  private type B = instrument.BaseD
  private type Q = instrument.QuoteD

  test("one encoder and decoder expose all ten exact delegates across both scenario families"):
    val entry = marketScenario(Side.Buy, 10, 100)
    val exit  = marketScenario(Side.Sell, 10, 102)
    val trip  = RoundTripScenario.create(instrument)(entry, exit).toOption.get

    assert(encoder.instrument.eq(instrument))
    assert(decoder.instrument.eq(instrument))
    assert(decoder.snapshot.eq(fixture.snapshot))

    val orderRecord: OrderScenarioRecord.V1                            = encoder.order(entry)
    val orderWire: Either[WireViolations[WireEncodeViolation], String] =
      encoder.encodeOrder(entry)
    val tripRecord: RoundTripScenarioRecord.V1                        = encoder.roundTrip(trip)
    val tripWire: Either[WireViolations[WireEncodeViolation], String] =
      encoder.encodeRoundTrip(trip)

    assertEquals(orderRecord, OrderScenarioRecord.fromScenario(instrument)(entry))
    assertEquals(orderWire, OrderScenarioRecord.encodeScenario(instrument)(entry))
    assertEquals(tripRecord, RoundTripScenarioRecord.fromScenario(instrument)(trip))
    assertEquals(tripWire, RoundTripScenarioRecord.encodeScenario(instrument)(trip))

    val reconstructedOrder: Either[
      OrderScenarioReconstructionFailure,
      OrderScenario[instrument.PositionD, instrument.BaseD, instrument.QuoteD, instrument.MarketState]
    ] =
      decoder.order(orderRecord)
    val decodedOrder: Either[
      OrderScenarioReconstructionFailure,
      OrderScenario[instrument.PositionD, instrument.BaseD, instrument.QuoteD, instrument.MarketState]
    ] =
      decoder.decodeOrder(orderWire.toOption.get)
    val orderBatch: Either[
      WireViolations[IndexedOrderScenarioReconstructionFailure],
      Vector[
        OrderScenario[instrument.PositionD, instrument.BaseD, instrument.QuoteD, instrument.MarketState]
      ]
    ] =
      decoder.orderBatch(Vector(orderWire.toOption.get, orderWire.toOption.get))

    assertEquals(
      reconstructedOrder.map(encoder.order),
      OrderScenarioRecord.reconstruct(orderRecord, instrument, fixture.snapshot).map(encoder.order)
    )
    assertEquals(
      decodedOrder.map(encoder.order),
      OrderScenarioRecord
        .decodeAndReconstruct(orderWire.toOption.get, instrument, fixture.snapshot)
        .map(encoder.order)
    )
    assertEquals(
      orderBatch.map(_.map(encoder.order)),
      OrderScenarioRecord.reconstructBatch(
        Vector(orderWire.toOption.get, orderWire.toOption.get),
        instrument,
        fixture.snapshot
      ).map(_.map(encoder.order))
    )

    val reconstructedTrip: Either[
      RoundTripScenarioReconstructionFailure,
      RoundTripScenario[instrument.PositionD, instrument.BaseD, instrument.QuoteD, instrument.MarketState]
    ] =
      decoder.roundTrip(tripRecord)
    val decodedTrip: Either[
      RoundTripScenarioReconstructionFailure,
      RoundTripScenario[instrument.PositionD, instrument.BaseD, instrument.QuoteD, instrument.MarketState]
    ] =
      decoder.decodeRoundTrip(tripWire.toOption.get)
    val tripBatch: Either[
      WireViolations[IndexedRoundTripScenarioReconstructionFailure],
      Vector[
        RoundTripScenario[instrument.PositionD, instrument.BaseD, instrument.QuoteD, instrument.MarketState]
      ]
    ] =
      decoder.roundTripBatch(Vector(tripWire.toOption.get, tripWire.toOption.get))

    assertEquals(
      reconstructedTrip.map(encoder.roundTrip),
      RoundTripScenarioRecord.reconstruct(tripRecord, instrument, fixture.snapshot).map(encoder.roundTrip)
    )
    assertEquals(
      decodedTrip.map(encoder.roundTrip),
      RoundTripScenarioRecord
        .decodeAndReconstruct(tripWire.toOption.get, instrument, fixture.snapshot)
        .map(encoder.roundTrip)
    )
    assertEquals(
      tripBatch.map(_.map(encoder.roundTrip)),
      RoundTripScenarioRecord.reconstructBatch(
        Vector(tripWire.toOption.get, tripWire.toOption.get),
        instrument,
        fixture.snapshot
      ).map(_.map(encoder.roundTrip))
    )

  test("bound and context-free operations preserve canonical wire diagnostics limits and null behavior"):
    val entry       = marketScenario(Side.Buy, 10, 100)
    val exit        = marketScenario(Side.Sell, 10, 102)
    val trip        = RoundTripScenario.create(instrument)(entry, exit).toOption.get
    val record      = encoder.order(entry)
    val wire        = encoder.encodeOrder(entry).toOption.get
    val tripRecord  = encoder.roundTrip(trip)
    val tripWire    = encoder.encodeRoundTrip(trip).toOption.get
    val invalid     = record.copy(slices = Vector.empty)
    val invalidWire = OrderScenarioRecord.encode(invalid).toOption.get
    val malformed   = "not-json"
    val futureWire  = wire.replaceFirst("\"schemaVersion\":1", "\"schemaVersion\":9")
    val constrained = limits(maxBatchRecords = 1)

    val golden =
      """{"payload":{"activationEvidence":{"kind":"immediate"},"order":{"activation":{"kind":"immediate"},"execution":{"kind":"market","timeInForce":"immediateOrCancel"},"instrumentId":"scenario-scope-primary","lotCoordinate":"10","positionEffect":"unrestricted","side":"buy"},"pricingResolution":{"kind":"direct"},"slices":[{"liquidity":"taker","lotCoordinate":"10","market":{"additionalConversions":[],"baseToSettle":{"denominator":"1","numerator":"100"},"priceCoordinate":"100","quoteToSettle":{"denominator":"1","numerator":"2"}}}]},"recordType":"trading.order-scenario","schemaVersion":1}"""
    assertEquals(wire, golden)

    assertEquals(OrderScenarioRecord.parse(s"  \n$wire\n"), Right(record))
    assertEquals(OrderScenarioRecord.encode(record), Right(wire))
    assertEquals(RoundTripScenarioRecord.parse(tripWire), Right(tripRecord))
    assertEquals(RoundTripScenarioRecord.encode(tripRecord), Right(tripWire))
    assert(OrderScenarioRecord.schema().isRight)
    assert(RoundTripScenarioRecord.schema().isRight)

    assertEquals(
      decoder.order(invalid),
      OrderScenarioRecord.reconstruct(invalid, instrument, fixture.snapshot)
    )
    assertEquals(
      decoder.decodeOrder(malformed, DecodeLimits.default, 7),
      OrderScenarioRecord.decodeAndReconstruct(
        malformed,
        instrument,
        fixture.snapshot,
        DecodeLimits.default,
        7
      )
    )
    assertEquals(
      decoder.decodeOrder(futureWire, DecodeLimits.default, 11),
      OrderScenarioRecord.decodeAndReconstruct(
        futureWire,
        instrument,
        fixture.snapshot,
        DecodeLimits.default,
        11
      )
    )
    assertEquals(
      decoder.orderBatch(Vector(wire, malformed, invalidWire), DecodeLimits.default),
      OrderScenarioRecord.reconstructBatch(
        Vector(wire, malformed, invalidWire),
        instrument,
        fixture.snapshot,
        DecodeLimits.default
      )
    )
    assertEquals(
      decoder.orderBatch(Vector(wire, malformed), constrained),
      OrderScenarioRecord.reconstructBatch(
        Vector(wire, malformed),
        instrument,
        fixture.snapshot,
        constrained
      )
    )
    assertEquals(
      decoder.roundTripBatch(Vector(tripWire, malformed, tripWire), DecodeLimits.default),
      RoundTripScenarioRecord.reconstructBatch(
        Vector(tripWire, malformed, tripWire),
        instrument,
        fixture.snapshot,
        DecodeLimits.default
      )
    )

    val indexed = decoder
      .orderBatch(Vector(wire, malformed, invalidWire))
      .left
      .toOption
      .get
      .toVector
    assertEquals(indexed.map(_.recordIndex), Vector(1, 2))
    indexed.head.failure match
      case OrderScenarioReconstructionFailure.Codec(errors) =>
        assertEquals(errors.head.recordIndex, 1)
        assertEquals(errors.head.path.render, "$")
      case other => fail(s"expected located codec failure, got $other")

    assertEquals(
      nullMessage(encoder.order(null)),
      nullMessage(OrderScenarioRecord.fromScenario(instrument)(null))
    )
    assertEquals(
      nullMessage(decoder.order(null)),
      nullMessage(OrderScenarioRecord.reconstruct(null, instrument, fixture.snapshot))
    )
    assertEquals(
      nullMessage(decoder.decodeOrder(null, DecodeLimits.default, 3)),
      nullMessage(
        OrderScenarioRecord.decodeAndReconstruct(
          null,
          instrument,
          fixture.snapshot,
          DecodeLimits.default,
          3
        )
      )
    )
    val missingSnapshot = ScenarioRecord.decoder(instrument, null)
    assertEquals(
      nullMessage(missingSnapshot.roundTrip(tripRecord)),
      nullMessage(RoundTripScenarioRecord.reconstruct(tripRecord, instrument, null))
    )

  test("captured snapshots and reusable contexts remain coherent independent and order-invariant"):
    val converted = marketScenario(
      Side.Buy,
      10,
      100,
      Vector(fixture.token -> Rational(3), fixture.rebate -> Rational(5))
    )
    val record        = encoder.order(converted)
    val wire          = encoder.encodeOrder(converted).toOption.get
    val emptySnapshot =
      CatalogRoot.create().initialState.snapshot
    val emptyDecoder = ScenarioRecord.decoder(instrument, emptySnapshot)

    assert(decoder.order(record).isRight)
    assertEquals(
      emptyDecoder.order(record),
      OrderScenarioRecord.reconstruct(record, instrument, emptySnapshot)
    )
    assert(emptyDecoder.order(record).isLeft)
    assert(decoder.order(record).isRight)

    val firstRecord  = encoder.order(converted)
    val failedMiddle = emptyDecoder.decodeOrder(wire)
    val secondRecord = encoder.order(converted)
    assertEquals(firstRecord, secondRecord)
    assert(firstRecord.ne(secondRecord), "scenario projection must not cache record instances")
    assert(failedMiddle.isLeft)
    assertEquals(
      decoder.decodeOrder(wire).map(encoder.order),
      OrderScenarioRecord
        .decodeAndReconstruct(wire, instrument, fixture.snapshot)
        .map(encoder.order)
    )

    val concurrentWires = Await.result(
      Future.sequence(Vector.fill(24)(Future(encoder.encodeOrder(converted)))),
      20.seconds
    )
    assert(concurrentWires.forall(_ == Right(wire)))

    val concurrentRecords = Await.result(
      Future.sequence(Vector.fill(24)(Future(encoder.order(converted)))),
      20.seconds
    )
    concurrentRecords.foreach(value => assertEquals(value, record))
    concurrentRecords.indices.foreach: left =>
      (left + 1).until(concurrentRecords.size).foreach: right =>
        assert(
          concurrentRecords(left).ne(concurrentRecords(right)),
          s"concurrent records $left and $right shared an instance"
        )

    val concurrentDecodes = Await.result(
      Future.sequence(Vector.fill(24)(Future(decoder.decodeOrder(wire)))),
      20.seconds
    )
    val direct = OrderScenarioRecord.decodeAndReconstruct(wire, instrument, fixture.snapshot)
    concurrentDecodes.foreach(value => assertEquals(value.map(encoder.order), direct.map(encoder.order)))

  private def marketScenario(
    side: Side,
    lotsCoordinate: BigInt,
    priceCoordinate: BigInt,
    additional: Vector[(Asset, Rational)] = Vector.empty
  ): OrderScenario[D, B, Q, instrument.MarketState] =
    val lots        = fixture.lots(instrument, lotsCoordinate)
    val price       = fixture.price(instrument, priceCoordinate)
    val conversions = additional.map: (source, rate) =>
      SettlementConversion.exact(instrument)(source)(rate).toOption.get
    val market = MarketState
      .fromAnchors(instrument)(
        price,
        price.coefficient * Rational(2),
        Rational(2),
        conversions
      )
      .toOption
      .get
    val slice = LiquiditySlice
      .create(instrument)(lots, market, LiquidityRole.Taker)
      .toOption
      .get
    val order       = Order.market(instrument)(side, lots).toOption.get
    val assumptions =
      ScenarioAssumptions.one(order)(order.activation.evidence, order.execution.resolution, slice)
    OrderScenario.evaluate(instrument)(assumptions).toOption.get
  end marketScenario

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

  private def nullMessage(value: => Any): String =
    intercept[NullPointerException](value).getMessage
end ScenarioRecordScopeSuite
