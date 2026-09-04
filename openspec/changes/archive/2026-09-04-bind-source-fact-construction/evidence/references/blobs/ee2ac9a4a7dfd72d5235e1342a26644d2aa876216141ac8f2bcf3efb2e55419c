package trading.execution

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

import munit.FunSuite

import trading.economics.instrument.*
import trading.order.Order
import trading.order.Side
import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

final class SourceFactScopeSuite extends FunSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear
  private val target     = executionTarget("source", "account")
  private val order      = required(Order.market(instrument)(Side.Buy, fixtures.lots(instrument, 10)))
  private val lifecycle  = required(
    ExecutionLifecycle.create(instrument)(
      order,
      id(ExecutionOrderId.from("logical-order")),
      id(OrderLineageId.from("lineage")),
      target
    )
  )
  private val sourceOrder = qualifiedOrder(target, "source-order")
  private val scope       = SourceFact.forLifecycle(lifecycle)

  private type PositionD = instrument.PositionD
  private type BaseD     = instrument.BaseD
  private type QuoteD    = instrument.QuoteD

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def violations[A](value: Either[SourceFactViolations, A]): SourceFactViolations =
    value.fold(identity, result => fail(s"expected violations, received $result"))

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def executionTarget(source: String, account: String): ExecutionTarget =
    required(ExecutionTarget.create(id(ExecutionSourceId.from(source)), id(ExecutionAccountId.from(account))))

  private def event(executionTarget: ExecutionTarget, value: String): QualifiedSourceEventId =
    required(QualifiedSourceEventId.create(executionTarget, id(NativeSourceEventId.from(value))))

  private def qualifiedOrder(executionTarget: ExecutionTarget, value: String): QualifiedSourceOrderId =
    required(QualifiedSourceOrderId.create(executionTarget, id(NativeSourceOrderId.from(value))))

  private def fillId(executionTarget: ExecutionTarget, value: String): QualifiedFillId =
    required(QualifiedFillId.create(executionTarget, id(NativeFillId.from(value))))

  private def stream(executionTarget: ExecutionTarget): QualifiedSourceStreamId =
    required(QualifiedSourceStreamId.create(executionTarget, id(SourceStreamId.from("orders"))))

  private def position(executionTarget: ExecutionTarget, sequence: BigInt): QualifiedStreamPosition =
    required(QualifiedStreamPosition.create(stream(executionTarget), id(SourceSequence.from(sequence))))

  private def sequenced(executionTarget: ExecutionTarget, sequence: BigInt): SourceOrdering =
    val at = position(executionTarget, sequence)
    required(SourceOrdering.sequenced(at, required(SourceContinuation.origin(at.stream))))

  private def checkpoint(executionTarget: ExecutionTarget, sequence: BigInt): SourceCheckpoint =
    val at = position(executionTarget, sequence)
    required(SourceCheckpoint.create(at, required(SourceContinuation.origin(at.stream))))

  private def completeness(executionTarget: ExecutionTarget, sequence: BigInt): SourceCompleteness =
    required(SourceCompleteness.create(position(executionTarget, sequence)))

  private def assertEquivalent[A <: SourceFact[?, ?, ?]](
    scoped: Either[SourceFactViolations, A],
    direct: Either[SourceFactViolations, A]
  ): Unit =
    assertEquals(scoped, direct)
    val scopedFact = required(scoped)
    val directFact = required(direct)
    assert(scopedFact.ne(directFact), "scope and direct construction must allocate independent facts")

  private lazy val sameInstrumentDifferentGrids: Instrument =
    val baseDefinition     = AssetDefinition(AssetId.from("btc").toOption.get, AtomId("asset:btc"))
    val quoteDefinition    = AssetDefinition(AssetId.from("usd").toOption.get, AtomId("asset:usd"))
    val positionDefinition = AssetDefinition(AssetId.from("contract").toOption.get, AtomId("asset:contract"))
    val baseKey            = DimKey.atom(baseDefinition.dimensionAtom)
    val quoteKey           = DimKey.atom(quoteDefinition.dimensionAtom)
    val positionKey        = DimKey.atom(positionDefinition.dimensionAtom)
    val priceKey           = DimKey.multiply(quoteKey, DimKey.inverse(baseKey))
    val lotsDefinition     = GridDefinition(
      GridIdentity(
        positionKey,
        GridKey(GridId.from("alternate-contract-lots").toOption.get, GridVersion.from(1).toOption.get)
      ),
      PositiveRational(Rational.one).toOption.get
    )
    val priceDefinition = GridDefinition(
      GridIdentity(
        priceKey,
        GridKey(GridId.from("alternate-usd-per-btc").toOption.get, GridVersion.from(1).toOption.get)
      ),
      PositiveRational(Rational.one).toOption.get
    )
    val batch = CatalogBatch.of(
      CatalogCommand.RegisterAsset(baseDefinition),
      CatalogCommand.RegisterAsset(quoteDefinition),
      CatalogCommand.RegisterAsset(positionDefinition),
      CatalogCommand.RegisterDimension(priceKey),
      CatalogCommand.RegisterGrid(lotsDefinition),
      CatalogCommand.RegisterGrid(priceDefinition)
    )
    val snapshot   = required(CatalogModel.commit(CatalogRoot.create().initialState, batch)).state.snapshot
    val definition = InstrumentDefinition(
      instrument.identity,
      AssetRoleIds(baseDefinition.id, quoteDefinition.id, positionDefinition.id, quoteDefinition.id),
      ListingDefinition(lotsDefinition.identity, priceDefinition.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )
    Instrument.fromSpec(required(InstrumentAssembler.assemble(definition, snapshot)))
  end sameInstrumentDifferentGrids

  test("one lifecycle scope constructs all nine precise fact forms as their direct owners do"):
    val orderingValue     = sequenced(target, 1)
    val lotsValue         = fixtures.lots(instrument, 2)
    val priceValue        = fixtures.price(instrument, Rational.one)
    val referencedFill    = fillId(target, "fill")
    val checkpointValue   = checkpoint(target, 2)
    val completenessValue = completeness(target, 3)

    assert(scope.lifecycle.eq(lifecycle))

    val accepted: Either[SourceFactViolations, OrderAccepted[PositionD, BaseD, QuoteD]] =
      scope.accepted(event(target, "accepted"), lifecycle.executionOrderId, sourceOrder, orderingValue)
    assertEquivalent(
      accepted,
      OrderAccepted.create(lifecycle)(
        event(target, "accepted"),
        lifecycle.executionOrderId,
        sourceOrder,
        orderingValue
      )
    )

    val rejected: Either[SourceFactViolations, OrderRejected[PositionD, BaseD, QuoteD]] =
      scope.rejected(event(target, "rejected"), lifecycle.executionOrderId, sourceOrder, ExplicitlyUnsequenced)
    assertEquivalent(
      rejected,
      OrderRejected.create(lifecycle)(
        event(target, "rejected"),
        lifecycle.executionOrderId,
        sourceOrder,
        ExplicitlyUnsequenced
      )
    )

    val fill: Either[SourceFactViolations, ExecutionFill[PositionD, BaseD, QuoteD]] =
      scope.fill(
        event(target, "fill"),
        lifecycle.executionOrderId,
        sourceOrder,
        referencedFill,
        lotsValue,
        priceValue,
        ExplicitlyUnsequenced
      )
    assertEquivalent(
      fill,
      ExecutionFill.create(lifecycle)(
        event(target, "fill"),
        lifecycle.executionOrderId,
        sourceOrder,
        referencedFill,
        lotsValue,
        priceValue,
        ExplicitlyUnsequenced
      )
    )

    val corrected: Either[SourceFactViolations, FillCorrected[PositionD, BaseD, QuoteD]] =
      scope.corrected(
        event(target, "corrected"),
        lifecycle.executionOrderId,
        sourceOrder,
        referencedFill,
        lotsValue,
        priceValue,
        ExplicitlyUnsequenced
      )
    assertEquivalent(
      corrected,
      FillCorrected.create(lifecycle)(
        event(target, "corrected"),
        lifecycle.executionOrderId,
        sourceOrder,
        referencedFill,
        lotsValue,
        priceValue,
        ExplicitlyUnsequenced
      )
    )

    val busted: Either[SourceFactViolations, FillBusted[PositionD, BaseD, QuoteD]] =
      scope.busted(
        event(target, "busted"),
        lifecycle.executionOrderId,
        sourceOrder,
        referencedFill,
        ExplicitlyUnsequenced
      )
    assertEquivalent(
      busted,
      FillBusted.create(lifecycle)(
        event(target, "busted"),
        lifecycle.executionOrderId,
        sourceOrder,
        referencedFill,
        ExplicitlyUnsequenced
      )
    )

    val cancelled: Either[SourceFactViolations, CancellationEffective[PositionD, BaseD, QuoteD]] =
      scope.cancellationEffective(
        event(target, "cancelled"),
        lifecycle.executionOrderId,
        sourceOrder,
        ExplicitlyUnsequenced
      )
    assertEquivalent(
      cancelled,
      CancellationEffective.create(lifecycle)(
        event(target, "cancelled"),
        lifecycle.executionOrderId,
        sourceOrder,
        ExplicitlyUnsequenced
      )
    )

    val reconciled: Either[SourceFactViolations, ReconciliationCheckpoint[PositionD, BaseD, QuoteD]] =
      scope.reconciliationCheckpoint(
        event(target, "checkpoint"),
        lifecycle.executionOrderId,
        sourceOrder,
        checkpointValue,
        ExplicitlyUnsequenced
      )
    assertEquivalent(
      reconciled,
      ReconciliationCheckpoint.create(lifecycle)(
        event(target, "checkpoint"),
        lifecycle.executionOrderId,
        sourceOrder,
        checkpointValue,
        ExplicitlyUnsequenced
      )
    )

    val completed: Either[SourceFactViolations, SourceOrderCompleted[PositionD, BaseD, QuoteD]] =
      scope.sourceOrderCompleted(
        event(target, "completed"),
        lifecycle.executionOrderId,
        sourceOrder,
        completenessValue,
        ExplicitlyUnsequenced
      )
    assertEquivalent(
      completed,
      SourceOrderCompleted.create(lifecycle)(
        event(target, "completed"),
        lifecycle.executionOrderId,
        sourceOrder,
        completenessValue,
        ExplicitlyUnsequenced
      )
    )

    val absent: Either[SourceFactViolations, SourceOrderAbsent[PositionD, BaseD, QuoteD]] =
      scope.sourceOrderAbsent(
        event(target, "absent"),
        lifecycle.executionOrderId,
        sourceOrder,
        completenessValue,
        ExplicitlyUnsequenced
      )
    assertEquivalent(
      absent,
      SourceOrderAbsent.create(lifecycle)(
        event(target, "absent"),
        lifecycle.executionOrderId,
        sourceOrder,
        completenessValue,
        ExplicitlyUnsequenced
      )
    )

  test("scoped construction preserves complete deterministic common and missing-value violations"):
    val foreignTarget  = executionTarget("foreign-source", "foreign-account")
    val foreignOrder   = id(ExecutionOrderId.from("foreign-logical-order"))
    val foreignSource  = qualifiedOrder(foreignTarget, "foreign-order")
    val foreignOrderBy = sequenced(foreignTarget, 1)

    val common       = scope.accepted(null, foreignOrder, foreignSource, foreignOrderBy)
    val directCommon = OrderAccepted.create(lifecycle)(
      null,
      foreignOrder,
      foreignSource,
      foreignOrderBy
    )
    assertEquals(common, directCommon)
    assertEquals(
      violations(common).toVector,
      Vector(
        MissingSourceFactValue(SourceFactLocation.Event),
        SourceFactLogicalOrderMismatch(lifecycle.executionOrderId, foreignOrder),
        SourceFactTargetMismatch(SourceFactLocation.SourceOrder, target, foreignTarget),
        SourceFactTargetMismatch(SourceFactLocation.Ordering, target, foreignTarget)
      )
    )

    val missing       = scope.fill(null, null, null, null, null, null, null)
    val directMissing = ExecutionFill.create(lifecycle)(null, null, null, null, null, null, null)
    assertEquals(missing, directMissing)
    assertEquals(
      violations(missing).toVector,
      Vector(
        MissingSourceFactValue(SourceFactLocation.Event),
        MissingSourceFactValue(SourceFactLocation.LogicalExecutionOrder),
        MissingSourceFactValue(SourceFactLocation.SourceOrder),
        MissingSourceFactValue(SourceFactLocation.Ordering),
        MissingSourceFactValue(SourceFactLocation.Fill),
        MissingSourceFactValue(SourceFactLocation.Lots),
        MissingSourceFactValue(SourceFactLocation.Price)
      )
    )

  test("scope association retains foreign logical, target, instrument, grid, and boundary checks"):
    val foreignTarget = executionTarget("foreign-source", "foreign-account")
    val foreignFill   = fillId(foreignTarget, "foreign-fill")
    val foreignLots   = fixtures.lots(fixtures.foreignIdentity, 2).asInstanceOf[Lots[PositionD]]
    val foreignPrice  = fixtures.price(fixtures.foreignIdentity, Rational.one).asInstanceOf[Price[BaseD, QuoteD]]
    val lotsValue     = fixtures.lots(instrument, 2)
    val priceValue    = fixtures.price(instrument, Rational.one)

    val fillResult = scope.fill(
      event(target, "foreign-economics"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignFill,
      foreignLots,
      foreignPrice,
      ExplicitlyUnsequenced
    )
    val directFill = ExecutionFill.create(lifecycle)(
      event(target, "foreign-economics"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignFill,
      foreignLots,
      foreignPrice,
      ExplicitlyUnsequenced
    )
    assertEquals(fillResult, directFill)
    assertEquals(
      violations(fillResult).toVector,
      Vector(
        SourceFactTargetMismatch(SourceFactLocation.Fill, target, foreignTarget),
        SourceFactInstrumentMismatch(SourceFactLocation.Lots, instrument.identity.id,
          fixtures.foreignIdentity.identity.id),
        SourceFactInstrumentMismatch(SourceFactLocation.Price, instrument.identity.id,
          fixtures.foreignIdentity.identity.id)
      )
    )

    val corrected = scope.corrected(
      event(target, "foreign-correction"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignFill,
      foreignLots,
      foreignPrice,
      ExplicitlyUnsequenced
    )
    val directCorrected = FillCorrected.create(lifecycle)(
      event(target, "foreign-correction"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignFill,
      foreignLots,
      foreignPrice,
      ExplicitlyUnsequenced
    )
    assertEquals(corrected, directCorrected)
    assertEquals(
      violations(corrected).toVector,
      Vector(
        SourceFactTargetMismatch(SourceFactLocation.CorrectionTarget, target, foreignTarget),
        SourceFactInstrumentMismatch(SourceFactLocation.Lots, instrument.identity.id,
          fixtures.foreignIdentity.identity.id),
        SourceFactInstrumentMismatch(SourceFactLocation.Price, instrument.identity.id,
          fixtures.foreignIdentity.identity.id)
      )
    )

    val busted = scope.busted(
      event(target, "foreign-bust"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignFill,
      ExplicitlyUnsequenced
    )
    val directBusted = FillBusted.create(lifecycle)(
      event(target, "foreign-bust"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignFill,
      ExplicitlyUnsequenced
    )
    assertEquals(busted, directBusted)
    assertEquals(
      violations(busted).toVector,
      Vector(SourceFactTargetMismatch(SourceFactLocation.BustTarget, target, foreignTarget))
    )

    val alternateLots  = Lots.fromCount(sameInstrumentDifferentGrids)(2).toOption.get.asInstanceOf[Lots[PositionD]]
    val alternatePrice = Price
      .exact(sameInstrumentDifferentGrids)(Rational.one)
      .toOption
      .get
      .asInstanceOf[Price[BaseD, QuoteD]]
    val wrongGrids = scope.fill(
      event(target, "wrong-grids"),
      lifecycle.executionOrderId,
      sourceOrder,
      fillId(target, "wrong-grids"),
      alternateLots,
      alternatePrice,
      ExplicitlyUnsequenced
    )
    val directWrongGrids = ExecutionFill.create(lifecycle)(
      event(target, "wrong-grids"),
      lifecycle.executionOrderId,
      sourceOrder,
      fillId(target, "wrong-grids"),
      alternateLots,
      alternatePrice,
      ExplicitlyUnsequenced
    )
    assertEquals(wrongGrids, directWrongGrids)
    assertEquals(
      violations(wrongGrids).toVector,
      Vector(
        SourceFactGridMismatch(SourceFactLocation.Lots, lifecycle.positionGrid.identity, alternateLots.grid.identity),
        SourceFactGridMismatch(SourceFactLocation.Price, lifecycle.instrument.priceGrid.identity,
          alternatePrice.grid.identity)
      )
    )

    val foreignCheckpoint = checkpoint(foreignTarget, 2)
    val checkpointResult  = scope.reconciliationCheckpoint(
      event(target, "foreign-checkpoint"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignCheckpoint,
      ExplicitlyUnsequenced
    )
    assertEquals(
      checkpointResult,
      ReconciliationCheckpoint.create(lifecycle)(
        event(target, "foreign-checkpoint"),
        lifecycle.executionOrderId,
        sourceOrder,
        foreignCheckpoint,
        ExplicitlyUnsequenced
      )
    )
    assertEquals(
      violations(checkpointResult).toVector,
      Vector(SourceFactTargetMismatch(SourceFactLocation.Checkpoint, target, foreignTarget))
    )

    val foreignCompleteness = completeness(foreignTarget, 3)
    val completed           = scope.sourceOrderCompleted(
      event(target, "foreign-complete"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignCompleteness,
      ExplicitlyUnsequenced
    )
    val absent = scope.sourceOrderAbsent(
      event(target, "foreign-absent"),
      lifecycle.executionOrderId,
      sourceOrder,
      foreignCompleteness,
      ExplicitlyUnsequenced
    )
    assertEquals(
      completed,
      SourceOrderCompleted.create(lifecycle)(
        event(target, "foreign-complete"),
        lifecycle.executionOrderId,
        sourceOrder,
        foreignCompleteness,
        ExplicitlyUnsequenced
      )
    )
    assertEquals(
      absent,
      SourceOrderAbsent.create(lifecycle)(
        event(target, "foreign-absent"),
        lifecycle.executionOrderId,
        sourceOrder,
        foreignCompleteness,
        ExplicitlyUnsequenced
      )
    )
    val completenessViolation =
      Vector(SourceFactTargetMismatch(SourceFactLocation.Completeness, target, foreignTarget))
    assertEquals(violations(completed).toVector, completenessViolation)
    assertEquals(violations(absent).toVector, completenessViolation)

    val validCorrection = scope.corrected(
      event(target, "valid-correction"),
      lifecycle.executionOrderId,
      sourceOrder,
      fillId(target, "valid-reference"),
      lotsValue,
      priceValue,
      ExplicitlyUnsequenced
    )
    assert(validCorrection.isRight)

  test("one scope is order-invariant and independently allocates under sequential and concurrent reuse"):
    val eventIdValue = event(target, "reused-fill")
    val fillIdValue  = fillId(target, "reused-fill")
    val lotsValue    = fixtures.lots(instrument, 2)
    val priceValue   = fixtures.price(instrument, Rational.one)

    def scopedFill: Either[SourceFactViolations, ExecutionFill[PositionD, BaseD, QuoteD]] =
      scope.fill(
        eventIdValue,
        lifecycle.executionOrderId,
        sourceOrder,
        fillIdValue,
        lotsValue,
        priceValue,
        ExplicitlyUnsequenced
      )

    val acceptedFirst = scope.accepted(
      event(target, "interleaved-accepted"),
      lifecycle.executionOrderId,
      sourceOrder,
      ExplicitlyUnsequenced
    )
    val firstFill      = required(scopedFill)
    val rejectedMiddle = scope.rejected(
      event(target, "interleaved-rejected"),
      lifecycle.executionOrderId,
      sourceOrder,
      ExplicitlyUnsequenced
    )
    val secondFill = required(scopedFill)

    assert(acceptedFirst.isRight)
    assert(rejectedMiddle.isRight)
    assertEquals(firstFill, secondFill)
    assert(firstFill.ne(secondFill))

    val concurrent = Await.result(Future.sequence(Vector.fill(32)(Future(scopedFill))), 20.seconds).map(required)
    val direct     = required(
      ExecutionFill.create(lifecycle)(
        eventIdValue,
        lifecycle.executionOrderId,
        sourceOrder,
        fillIdValue,
        lotsValue,
        priceValue,
        ExplicitlyUnsequenced
      )
    )
    concurrent.foreach(value => assertEquals(value, direct))
    concurrent.indices.foreach: left =>
      (left + 1).until(concurrent.size).foreach: right =>
        assert(concurrent(left).ne(concurrent(right)), s"concurrent facts $left and $right shared an instance")
end SourceFactScopeSuite
