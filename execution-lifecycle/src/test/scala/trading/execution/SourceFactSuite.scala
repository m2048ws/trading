package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.lang.reflect.Modifier

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.economics.instrument.Lots
import trading.economics.instrument.Price
import trading.order.Order
import trading.order.Side
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational

final class SourceFactSuite extends ScalaCheckSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear
  private val target     = executionTarget("source", "account")
  private val order      = Order.market(instrument)(Side.Buy, fixtures.lots(instrument, 10)).toOption.get
  private val lifecycle  = required(
    ExecutionLifecycle.create(instrument)(
      order,
      id(ExecutionOrderId.from("logical-order")),
      id(OrderLineageId.from("lineage")),
      target
    )
  )
  private val sourceOrder = qualifiedOrder(target, "source-order")

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

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

  private def fill(
    eventValue: String,
    fillValue: String,
    lotCount: BigInt = 2,
    ordering: SourceOrdering = SourceOrdering.unsequenced
  ): ExecutionFill[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] =
    required(
      ExecutionFill.create(lifecycle)(
        event(target, eventValue),
        lifecycle.executionOrderId,
        sourceOrder,
        fillId(target, fillValue),
        fixtures.lots(instrument, lotCount),
        fixtures.price(instrument, Rational.one),
        ordering
      )
    )

  private def initial: SourceEvidenceState[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(SourceEvidenceState.initial(lifecycle))

  private def recorded(
    transition: SourceFactTransition[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D
    ]
  ): SourceFactRecorded[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = transition match
    case value: SourceFactRecorded[?, ?, ?] => value.asInstanceOf[
        SourceFactRecorded[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D
        ]
      ]
    case rejected: SourceFactRejected[?, ?, ?] => fail(s"unexpected rejection: ${rejected.violations}")

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("source facts form a closed vocabulary with qualified native provenance and explicit ordering"):
    val ordering = sequenced(target, 1)
    val accepted = required(
      OrderAccepted.create(lifecycle)(
        event(target, "accepted"),
        lifecycle.executionOrderId,
        sourceOrder,
        ordering
      )
    )
    val rejected = required(
      OrderRejected.create(lifecycle)(
        event(target, "rejected"),
        lifecycle.executionOrderId,
        sourceOrder,
        SourceOrdering.unsequenced
      )
    )
    val executionFill = fill("fill", "fill-1")
    val correction    = required(
      FillCorrected.create(lifecycle)(
        event(target, "correction"),
        lifecycle.executionOrderId,
        sourceOrder,
        executionFill.fillId,
        fixtures.lots(instrument, 3),
        fixtures.price(instrument, Rational.one),
        SourceOrdering.unsequenced
      )
    )
    val bust = required(
      FillBusted.create(lifecycle)(
        event(target, "bust"),
        lifecycle.executionOrderId,
        sourceOrder,
        executionFill.fillId,
        SourceOrdering.unsequenced
      )
    )
    val cancelled = required(
      CancellationEffective.create(lifecycle)(
        event(target, "cancelled"),
        lifecycle.executionOrderId,
        sourceOrder,
        SourceOrdering.unsequenced
      )
    )
    val atTwo        = position(target, 2)
    val continuation = required(SourceContinuation.after(position(target, 1)))
    val checkpoint   = required(SourceCheckpoint.create(atTwo, continuation))
    val completeness = required(SourceCompleteness.create(atTwo))
    val reconciled   = required(
      ReconciliationCheckpoint.create(lifecycle)(
        event(target, "checkpoint"),
        lifecycle.executionOrderId,
        sourceOrder,
        checkpoint,
        SourceOrdering.unsequenced
      )
    )
    val completed = required(
      SourceOrderCompleted.create(lifecycle)(
        event(target, "complete"),
        lifecycle.executionOrderId,
        sourceOrder,
        completeness,
        SourceOrdering.unsequenced
      )
    )
    val absent = required(
      SourceOrderAbsent.create(lifecycle)(
        event(target, "absent"),
        lifecycle.executionOrderId,
        sourceOrder,
        completeness,
        SourceOrdering.unsequenced
      )
    )

    val alternatives: Vector[SourceFact[?, ?, ?]] =
      Vector(accepted, rejected, executionFill, correction, bust, cancelled, reconciled, completed, absent)
    val kinds = alternatives.map:
      case _: OrderAccepted[?, ?, ?]            => "accepted"
      case _: OrderRejected[?, ?, ?]            => "rejected"
      case _: ExecutionFill[?, ?, ?]            => "fill"
      case _: FillCorrected[?, ?, ?]            => "corrected"
      case _: FillBusted[?, ?, ?]               => "busted"
      case _: CancellationEffective[?, ?, ?]    => "cancelled"
      case _: ReconciliationCheckpoint[?, ?, ?] => "checkpoint"
      case _: SourceOrderCompleted[?, ?, ?]     => "complete"
      case _: SourceOrderAbsent[?, ?, ?]        => "absent"

    assertEquals(kinds,
      Vector("accepted", "rejected", "fill", "corrected", "busted", "cancelled", "checkpoint", "complete", "absent"))
    alternatives.foreach: fact =>
      assertEquals(fact.target, target)
      assertEquals(fact.sourceOrderId.target, target)
      assertEquals(fact.executionOrderId, lifecycle.executionOrderId)
    assertEquals(accepted.authoritativePosition, Some(position(target, 1)))
    assertEquals(rejected.authoritativePosition, None)

  test("fills retain positive instrument lots, exact grid price, and qualified fill/source-order provenance"):
    val value = fill("fill", "native-fill", 4)

    assertEquals(value.fillId.target, target)
    assertEquals(value.fillId.native, id(NativeFillId.from("native-fill")))
    assertEquals(value.sourceOrderId, sourceOrder)
    assertEquals(value.lots, fixtures.lots(instrument, 4))
    assertEquals(value.lots.grid.identity, lifecycle.positionGrid.identity)
    assertEquals(value.price, fixtures.price(instrument, Rational.one))
    assertEquals(value.price.grid.identity, lifecycle.instrument.priceGrid.identity)

  test("event and fill indexes distinguish replay, identity conflict, and equal economics under distinct fill IDs"):
    val original = fill("event-1", "fill-1", 2)
    val first    = recorded(initial.record(original))
    val replay   = recorded(first.state.record(fill("event-1", "fill-1", 2)))
    val distinct = fill("event-2", "fill-2", 2)
    val second   = recorded(replay.state.record(distinct))
    val conflict = fill("event-3", "fill-1", 3)
    val third    = recorded(second.state.record(conflict))

    assertEquals(first.classifications.toVector, Vector(SourceFactClassification.Applied))
    assertEquals(replay.classifications.toVector, Vector(SourceFactClassification.DuplicateSourceEvent))
    assertEquals(replay.state, first.state)
    assertEquals(second.state.fillsById.keySet, Set(original.fillId, distinct.fillId))
    assertEquals(second.state.fillConflicts, Vector.empty)
    assertEquals(
      third.classifications.toVector,
      Vector(SourceFactClassification.Applied, SourceFactClassification.ConflictingFillIdentity)
    )
    assertEquals(third.state.fillsById(original.fillId), original)
    assertEquals(third.state.fillConflicts.size, 1)
    assertEquals(third.state.fillConflicts.head.conflicting, conflict)

  test("same source-event identity with different content retains a typed conflict and preserves the original"):
    val nativeEvent = event(target, "event")
    val accepted    = required(
      OrderAccepted.create(lifecycle)(
        nativeEvent,
        lifecycle.executionOrderId,
        sourceOrder,
        SourceOrdering.unsequenced
      )
    )
    val rejected = required(
      OrderRejected.create(lifecycle)(
        nativeEvent,
        lifecycle.executionOrderId,
        sourceOrder,
        SourceOrdering.unsequenced
      )
    )
    val first      = recorded(initial.record(accepted))
    val conflicted = recorded(first.state.record(rejected))

    assertEquals(
      conflicted.classifications.toVector,
      Vector(SourceFactClassification.ConflictingSourceEvent)
    )
    assertEquals(conflicted.state.factsByEvent(nativeEvent), accepted)
    assertEquals(conflicted.state.eventConflicts.size, 1)
    assertEquals(conflicted.state.eventConflicts.head.conflicting, rejected)

  test("source/account qualification keeps equal native fill identity distinct and rejects foreign scope"):
    val foreignTarget = executionTarget("other-source", "other-account")
    val localFillId   = fillId(target, "same-native")
    val foreignFillId = fillId(foreignTarget, "same-native")
    assertNotEquals(localFillId, foreignFillId)

    val foreignEvent = event(foreignTarget, "foreign")
    val foreignOrder = qualifiedOrder(foreignTarget, "source-order")
    val result       = ExecutionFill.create(lifecycle)(
      foreignEvent,
      lifecycle.executionOrderId,
      foreignOrder,
      foreignFillId,
      fixtures.lots(instrument, 1),
      fixtures.price(instrument, Rational.one),
      sequenced(foreignTarget, 1)
    )
    assertEquals(
      result.left.map(_.toVector),
      Left(
        Vector(
          SourceFactTargetMismatch(SourceFactLocation.Event, target, foreignTarget),
          SourceFactTargetMismatch(SourceFactLocation.SourceOrder, target, foreignTarget),
          SourceFactTargetMismatch(SourceFactLocation.Ordering, target, foreignTarget),
          SourceFactTargetMismatch(SourceFactLocation.Fill, target, foreignTarget)
        )
      )
    )

    val foreignInstrument = fixtures.foreignIdentity
    val foreignEconomics  = ExecutionFill.create(lifecycle)(
      event(target, "foreign-economics"),
      lifecycle.executionOrderId,
      sourceOrder,
      fillId(target, "foreign-economics"),
      fixtures
        .lots(foreignInstrument, 1)
        .asInstanceOf[Lots[instrument.roles.position.D]],
      fixtures
        .price(foreignInstrument, Rational.one)
        .asInstanceOf[Price[instrument.roles.base.D, instrument.roles.quote.D]],
      SourceOrdering.unsequenced
    )
    assertEquals(
      foreignEconomics.left.map(_.toVector),
      Left(
        Vector(
          SourceFactInstrumentMismatch(
            SourceFactLocation.Lots,
            instrument.identity.id,
            foreignInstrument.identity.id
          ),
          SourceFactInstrumentMismatch(
            SourceFactLocation.Price,
            instrument.identity.id,
            foreignInstrument.identity.id
          )
        )
      )
    )

  test("multiple facts claiming one authoritative stream position remain visible as a position conflict"):
    val ordering = sequenced(target, 5)
    val accepted = required(
      OrderAccepted.create(lifecycle)(
        event(target, "accepted"),
        lifecycle.executionOrderId,
        sourceOrder,
        ordering
      )
    )
    val executionFill = fill("fill", "fill", 1, ordering)
    val first         = recorded(initial.record(accepted))
    val second        = recorded(first.state.record(executionFill))
    val atFive        = position(target, 5)

    assert(second.classifications.contains(SourceFactClassification.ConflictingStreamPosition))
    assertEquals(second.state.positionClaimants(atFive), Vector(accepted, executionFill))
    assertEquals(second.state.positionConflicts(atFive).claimants, Vector(accepted, executionFill))

  test("correction and bust arriving before their fill remain unresolved until the target is retained"):
    val targetFill = fill("fill", "fill", 2)
    val correction = required(
      FillCorrected.create(lifecycle)(
        event(target, "correction"),
        lifecycle.executionOrderId,
        sourceOrder,
        targetFill.fillId,
        fixtures.lots(instrument, 3),
        fixtures.price(instrument, Rational.one),
        SourceOrdering.unsequenced
      )
    )
    val bust = required(
      FillBusted.create(lifecycle)(
        event(target, "bust"),
        lifecycle.executionOrderId,
        sourceOrder,
        targetFill.fillId,
        SourceOrdering.unsequenced
      )
    )
    val corrected = recorded(initial.record(correction))
    val busted    = recorded(corrected.state.record(bust))

    assertEquals(busted.state.unresolvedFillReferences(targetFill.fillId).map(_.modifier), Vector(correction, bust))
    val resolved = recorded(busted.state.record(targetFill))
    assert(!resolved.state.unresolvedFillReferences.contains(targetFill.fillId))
    assertEquals(resolved.state.factsByEvent.values.toSet, Set(correction, bust, targetFill))

  property("replaying one normalized source fact never grows any source index"):
    forAll { (rawAttempts: Int) =>
      val attempts = (BigInt(rawAttempts).abs % 100).toInt + 1
      val original = fill("stable-event", "stable-fill", 2)
      val state    = 0.until(attempts).foldLeft(initial): (current, _) =>
        current.record(original).state

      state.factsByEvent == Map(original.eventId -> original) &&
      state.fillsById == Map(original.fillId -> original) &&
      state.eventConflicts.isEmpty && state.fillConflicts.isEmpty
    }

  test("source fact representations are final values that reject Java serialization"):
    val executionFill = fill("fill", "fill")
    val correction    = required(
      FillCorrected.create(lifecycle)(
        event(target, "correction"),
        lifecycle.executionOrderId,
        sourceOrder,
        executionFill.fillId,
        fixtures.lots(instrument, 3),
        fixtures.price(instrument, Rational.one),
        SourceOrdering.unsequenced
      )
    )
    val transition      = recorded(initial.record(correction))
    val representations = List(
      classOf[SourceFactViolations],
      classOf[OrderAccepted[?, ?, ?]],
      classOf[OrderRejected[?, ?, ?]],
      classOf[ExecutionFill[?, ?, ?]],
      classOf[FillCorrected[?, ?, ?]],
      classOf[FillBusted[?, ?, ?]],
      classOf[CancellationEffective[?, ?, ?]],
      classOf[ReconciliationCheckpoint[?, ?, ?]],
      classOf[SourceOrderCompleted[?, ?, ?]],
      classOf[SourceOrderAbsent[?, ?, ?]],
      classOf[SourceFactClassifications],
      classOf[SourceFactConflict[?, ?, ?]],
      classOf[FillIdentityConflict[?, ?, ?]],
      classOf[StreamPositionConflict[?, ?, ?]],
      classOf[UnresolvedFillReference[?, ?, ?]],
      classOf[SourceFactRecorded[?, ?, ?]],
      classOf[SourceFactRejected[?, ?, ?]],
      classOf[SourceEvidenceState[?, ?, ?]]
    )
    representations.foreach: representation =>
      assert(Modifier.isFinal(representation.getModifiers), s"${representation.getName} must be final")

    val values: List[JavaSerializationUnsupported] = List(
      executionFill,
      correction,
      transition,
      transition.state,
      transition.classifications,
      transition.state.unresolvedFillReferences(executionFill.fillId).head,
      MissingSourceFactValue(SourceFactLocation.Fill),
      SourceFactClassification.Applied
    )
    values.foreach(assertSerializationRejected)

end SourceFactSuite
