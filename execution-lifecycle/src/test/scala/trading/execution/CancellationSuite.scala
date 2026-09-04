package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.economics.instrument.PositionLots
import trading.order.Order
import trading.order.Side
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational

final class CancellationSuite extends ScalaCheckSuite:
  private val fixtures    = new InstrumentFixtures
  private val instrument  = fixtures.linear
  private val target      = executionTarget("source", "account")
  private val sourceOrder = required(
    QualifiedSourceOrderId.create(target, id(NativeSourceOrderId.from("source-order")))
  )
  private val sourceStream = required(
    QualifiedSourceStreamId.create(target, id(SourceStreamId.from("orders")))
  )

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def executionTarget(source: String, account: String): ExecutionTarget =
    required(ExecutionTarget.create(id(ExecutionSourceId.from(source)), id(ExecutionAccountId.from(account))))

  private def lifecycle(
    side: Side,
    orderedLots: BigInt,
    executionOrder: String,
    lineage: String = "lineage"
  ): ExecutionLifecycle[D, B, Q] =
    val order = required(Order.market(instrument)(side, fixtures.lots(instrument, orderedLots)))
    required(
      ExecutionLifecycle.create(instrument)(
        order,
        id(ExecutionOrderId.from(executionOrder)),
        id(OrderLineageId.from(lineage)),
        target
      )
    )

  private def event(value: String): QualifiedSourceEventId =
    required(QualifiedSourceEventId.create(target, id(NativeSourceEventId.from(value))))

  private def fillId(value: String): QualifiedFillId =
    required(QualifiedFillId.create(target, id(NativeFillId.from(value))))

  private def ordering(sequence: BigInt): AuthoritativelySequenced =
    val position = required(QualifiedStreamPosition.create(sourceStream, id(SourceSequence.from(sequence))))
    required(SourceOrdering.sequenced(position, required(SourceContinuation.origin(sourceStream))))

  private def submit(value: ExecutionLifecycle[D, B, Q], command: String): SubmitOrderCommand[D, B, Q] =
    required(SubmitOrderCommand.create(value)(id(ApplicationCommandId.from(command))))

  private def cancel(
    value: ExecutionLifecycle[D, B, Q],
    command: String,
    originalSubmit: ApplicationCommandId
  ): CancelOrderCommand[D, B, Q] =
    required(CancelOrderCommand.create(value)(id(ApplicationCommandId.from(command)), originalSubmit))

  private def cancellation(
    value: ExecutionLifecycle[D, B, Q],
    eventValue: String,
    sourceOrdering: SourceOrdering
  ): CancellationEffective[D, B, Q] =
    required(
      CancellationEffective.create(value)(
        event(eventValue),
        value.executionOrderId,
        sourceOrder,
        sourceOrdering
      )
    )

  private def fill(
    value: ExecutionLifecycle[D, B, Q],
    eventValue: String,
    fillValue: String,
    lots: BigInt,
    sourceOrdering: SourceOrdering
  ): ExecutionFill[D, B, Q] =
    required(
      ExecutionFill.create(value)(
        event(eventValue),
        value.executionOrderId,
        sourceOrder,
        fillId(fillValue),
        fixtures.lots(instrument, lots),
        fixtures.price(instrument, Rational.one),
        sourceOrdering
      )
    )

  private def correction(
    value: ExecutionLifecycle[D, B, Q],
    eventValue: String,
    targetFill: QualifiedFillId,
    lots: BigInt,
    sourceOrdering: SourceOrdering
  ): FillCorrected[D, B, Q] =
    required(
      FillCorrected.create(value)(
        event(eventValue),
        value.executionOrderId,
        sourceOrder,
        targetFill,
        fixtures.lots(instrument, lots),
        fixtures.price(instrument, Rational(BigInt(2))),
        sourceOrdering
      )
    )

  private def bust(
    value: ExecutionLifecycle[D, B, Q],
    eventValue: String,
    targetFill: QualifiedFillId,
    sourceOrdering: SourceOrdering
  ): FillBusted[D, B, Q] =
    required(
      FillBusted.create(value)(
        event(eventValue),
        value.executionOrderId,
        sourceOrder,
        targetFill,
        sourceOrdering
      )
    )

  private def initial(value: ExecutionLifecycle[D, B, Q]): ExecutionState[D, B, Q] =
    required(ExecutionState.initial(value))

  private def accepted(value: LifecycleTransition[D, B, Q]): LifecycleAccepted[D, B, Q] = value match
    case transition: LifecycleAccepted[?, ?, ?] => transition.asInstanceOf[LifecycleAccepted[D, B, Q]]
    case transition: LifecycleRejected[?, ?, ?] => fail(s"unexpected rejection: ${transition.rejection}")

  private def record(
    value: ExecutionLifecycle[D, B, Q],
    commands: Vector[ExecutionCommand[D, B, Q]] = Vector.empty,
    facts: Vector[SourceFact[D, B, Q]] = Vector.empty
  ): ExecutionState[D, B, Q] =
    val commanded = commands.foldLeft(initial(value))((state, command) => accepted(state.record(command)).state)
    facts.foldLeft(commanded)((state, fact) => accepted(state.record(fact)).state)

  private def position(value: ExecutionLifecycle[D, B, Q], coordinate: BigInt): PositionLots[D] =
    PositionLots.fromCoordinate(value.instrument)(coordinate).asInstanceOf[PositionLots[D]]

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("cancellation request and authoritative effectiveness remain distinct retained knowledge"):
    val value       = lifecycle(Side.Buy, 10, "request")
    val submitOrder = submit(value, "submit")
    val cancelOrder = cancel(value, "cancel", submitOrder.commandId)
    val requested   = record(value, Vector(submitOrder, cancelOrder)).observation

    requested.cancellationKnowledge match
      case Some(knowledge: CancellationRequested[?, ?, ?]) =>
        assertEquals(knowledge.evidence.issuedRequests, Set(cancelOrder))
        assertEquals(knowledge.evidence.referencedSubmissions, Set(submitOrder))
        assertEquals(knowledge.evidence.authoritativeConfirmations, Set.empty)
        assertSerializationRejected(knowledge)
      case other => fail(s"expected requested cancellation, received $other")
    val replayed = required(
      ExecutionState.replay(value)(Vector(cancelOrder, submitOrder), Vector.empty, Vector.empty)
    )
    assertEquals(replayed.state, record(value, Vector(submitOrder, cancelOrder)))
    assertEquals(replayed.rejections, Vector.empty)

    val confirmedFact = cancellation(value, "cancelled", SourceOrdering.unsequenced)
    val confirmed     = record(value, Vector(submitOrder, cancelOrder), Vector(confirmedFact)).observation
    confirmed.cancellationKnowledge match
      case Some(knowledge: CancellationConfirmed[?, ?, ?]) =>
        assertEquals(knowledge.evidence.issuedRequests, Set(cancelOrder))
        assertEquals(knowledge.evidence.authoritativeConfirmations, Set(confirmedFact))
        assertSerializationRejected(knowledge)
      case other => fail(s"expected confirmed cancellation, received $other")

  property("source ordering, not network delivery, identifies only the provably post-cancellation partial fill"):
    forAll { (reverse: Boolean) =>
      val value       = lifecycle(Side.Buy, 10, "race")
      val before      = fill(value, "before", "before", 2, ordering(1))
      val cancelled   = cancellation(value, "cancelled", ordering(2))
      val after       = fill(value, "after", "after", 3, ordering(3))
      val facts       = Vector[SourceFact[D, B, Q]](after, cancelled, before)
      val supplied    = if reverse then facts.reverse else facts
      val replay      = required(ExecutionState.replay(value)(Vector.empty, Vector.empty, supplied))
      val observation = replay.state.observation

      replay.rejections.isEmpty &&
      observation.effectiveFillLedger.knownExposure == position(value, 5) &&
      observation.anomalies.postCancellationFills.map(_.fillId) == Vector(after.fillId) &&
      observation.anomalies.postCancellationFills.head.exactExposure == position(value, 3)
    }

  test("an unsequenced fill delivered after a cancellation message makes no unsupported race claim"):
    val value     = lifecycle(Side.Buy, 10, "unsequenced")
    val cancelled = cancellation(value, "cancelled", ordering(1))
    val late      = fill(value, "late", "late", 2, SourceOrdering.unsequenced)
    val state     = record(value, facts = Vector(cancelled, late))

    assertEquals(state.observation.effectiveFillLedger.knownExposure, position(value, 2))
    assertEquals(state.observation.anomalies.postCancellationFills, Vector.empty)
    assert(state.observation.cancellationKnowledge.exists(_.isInstanceOf[CancellationConfirmed[?, ?, ?]]))

  test("post-cancellation anomaly exposure follows authoritative correction and bust effects"):
    val value       = lifecycle(Side.Sell, 10, "modifier-race")
    val cancelled   = cancellation(value, "cancelled", ordering(1))
    val original    = fill(value, "fill", "fill", 2, ordering(2))
    val replacement = correction(value, "correction", original.fillId, 4, ordering(3))
    val corrected   = record(value, facts = Vector(replacement, original, cancelled)).observation

    assertEquals(corrected.effectiveFillLedger.knownExposure, position(value, -4))
    assertEquals(corrected.anomalies.postCancellationFills.map(_.exactExposure), Vector(position(value, -4)))

    val finalBust = bust(value, "bust", original.fillId, ordering(4))
    val busted    = record(value, facts = Vector(finalBust, replacement, cancelled, original)).observation
    assertEquals(busted.effectiveFillLedger.knownExposure, position(value, 0))
    assertEquals(busted.anomalies.postCancellationFills, Vector.empty)
    assert(busted.effectiveFillLedger.byFillId(original.fillId).isInstanceOf[BustedEffectiveFill[?, ?, ?]])

  test("overfill, post-cancellation exposure, and unrelated source conflicts compose without deleting evidence"):
    val value        = lifecycle(Side.Buy, 5, "composed")
    val cancelled    = cancellation(value, "cancelled", ordering(0))
    val overfill     = fill(value, "overfill", "overfill", 7, ordering(1))
    val conflictId   = event("conflict")
    val acceptedFact = required(
      OrderAccepted.create(value)(conflictId, value.executionOrderId, sourceOrder, SourceOrdering.unsequenced)
    )
    val rejectedFact = required(
      OrderRejected.create(value)(conflictId, value.executionOrderId, sourceOrder, SourceOrdering.unsequenced)
    )
    val observation = record(value, facts = Vector(cancelled, overfill, acceptedFact, rejectedFact)).observation

    assertEquals(observation.effectiveFillLedger.knownExposure, position(value, 7))
    assertEquals(observation.anomalies.overfill.map(_.excessExposure), Some(position(value, 2)))
    assert(
      observation.effectiveFillLedger.overfill.get.asInstanceOf[AnyRef]
        .eq(observation.anomalies.overfill.get.asInstanceOf[AnyRef])
    )
    assertEquals(observation.anomalies.postCancellationFills.map(_.fillId), Vector(overfill.fillId))
    assertEquals(observation.anomalies.sourceEventConflicts.size, 1)

  test("conflicting authoritative positions do not prove relative cancellation and fill order"):
    val value        = lifecycle(Side.Buy, 10, "position-conflict")
    val samePosition = ordering(1)
    val cancelled    = cancellation(value, "cancelled", samePosition)
    val claimant     = fill(value, "claimant", "claimant", 2, samePosition)
    val later        = fill(value, "later", "later", 3, ordering(2))
    val observation  = record(value, facts = Vector(cancelled, claimant, later)).observation

    assert(observation.cancellationKnowledge.exists(_.isInstanceOf[CancellationConfirmed[?, ?, ?]]))
    assertEquals(observation.anomalies.postCancellationFills, Vector.empty)
    assertEquals(observation.effectiveFillLedger.knownExposure, position(value, 5))
    assertEquals(observation.anomalies.streamPositionConflicts.size, 1)

  test("a conflicting cancellation event does not manufacture effective cancellation"):
    val value      = lifecycle(Side.Buy, 10, "cancel-conflict")
    val conflictId = event("conflict")
    val cancelled  = required(
      CancellationEffective.create(value)(
        conflictId,
        value.executionOrderId,
        sourceOrder,
        SourceOrdering.unsequenced
      )
    )
    val acceptedFact = required(
      OrderAccepted.create(value)(conflictId, value.executionOrderId, sourceOrder, SourceOrdering.unsequenced)
    )
    val observation = record(value, facts = Vector(cancelled, acceptedFact)).observation

    observation.cancellationKnowledge match
      case Some(knowledge: CancellationConflicted[?, ?, ?]) =>
        assertEquals(knowledge.evidence.authoritativeConfirmations, Set.empty)
        assertEquals(knowledge.evidence.reportedConfirmations, Set(cancelled))
        assertSerializationRejected(knowledge)
      case other => fail(s"expected conflicted cancellation, received $other")

  test("confirmed cancel then submitted successor creates a mechanism-neutral immutable lineage link"):
    val predecessor      = lifecycle(Side.Buy, 10, "predecessor")
    val successor        = lifecycle(Side.Buy, 8, "successor")
    val predecessorState = record(
      predecessor,
      facts = Vector(cancellation(predecessor, "cancelled", SourceOrdering.unsequenced))
    )
    val successorSubmit = submit(successor, "successor-submit")
    val successorState  = record(successor, commands = Vector(successorSubmit))
    val link            = required(OrderLineageLink.create(predecessorState, successorState))

    assertEquals(link.lineageId, predecessor.lineageId)
    assertEquals(link.predecessorExecutionOrderId, predecessor.executionOrderId)
    assertEquals(link.successorExecutionOrderId, successor.executionOrderId)
    assertNotEquals(link.predecessorExecutionOrderId, link.successorExecutionOrderId)
    assertEquals(link.successorSubmissions.size, 1)
    assert(link.successorSubmissions.contains(successorSubmit))
    assertEquals(link.predecessor, predecessor)
    assertEquals(link.successor, successor)

  test("lineage construction accumulates independent identity, instrument, cancellation, and submission failures"):
    val predecessor      = lifecycle(Side.Buy, 10, "same-order", "first-lineage")
    val foreign          = fixtures.foreignIdentity
    val foreignOrder     = required(Order.market(foreign)(Side.Buy, fixtures.lots(foreign, 10)))
    val foreignLifecycle = required(
      ExecutionLifecycle.create(foreign)(
        foreignOrder,
        predecessor.executionOrderId,
        id(OrderLineageId.from("second-lineage")),
        target
      )
    )
    val foreignState = required(ExecutionState.initial(foreignLifecycle))
    val result       = OrderLineageLink.create(initial(predecessor), foreignState)

    assertEquals(
      result.left.map(_.toVector),
      Left(
        Vector(
          SameLineageExecutionOrder(predecessor.executionOrderId),
          LineageIdentityMismatch(predecessor.lineageId, foreignLifecycle.lineageId),
          LineageInstrumentMismatch(predecessor.instrumentId, foreignLifecycle.instrumentId),
          PredecessorCancellationNotConfirmed(predecessor.executionOrderId),
          SuccessorSubmissionNotRecorded(foreignLifecycle.executionOrderId)
        )
      )
    )

  test("cancellation, anomalies, and lineage retain structural replay equality"):
    val predecessor = lifecycle(Side.Buy, 5, "representation-predecessor")
    val successor   = lifecycle(Side.Buy, 5, "representation-successor")
    val cancelled   = cancellation(predecessor, "cancelled", ordering(0))
    val postFill    = fill(predecessor, "post", "post", 6, ordering(1))
    val first       = record(predecessor, facts = Vector(cancelled, postFill)).observation
    val replayed    = required(
      ExecutionState.replay(predecessor)(Vector.empty, Vector.empty, Vector(postFill, cancelled))
    ).state.observation
    val successorSubmit = submit(successor, "successor-submit")
    val link            = required(
      OrderLineageLink.create(
        record(predecessor, facts = Vector(cancelled)),
        record(successor, commands = Vector(successorSubmit))
      )
    )
    assertEquals(first, replayed)
    assertEquals(first.hashCode, replayed.hashCode)
    assertEquals(first.cancellationKnowledge.get.evidence.copy(), first.cancellationKnowledge.get.evidence)
    assertEquals(first.anomalies.copy(), first.anomalies)
    assertEquals(first.anomalies.postCancellationFills.head.copy(), first.anomalies.postCancellationFills.head)
    assertEquals(first.anomalies.postCancellationFills.head.fillId, postFill.fillId)
    val values = List[JavaSerializationUnsupported](
      first.cancellationKnowledge.get,
      first.cancellationKnowledge.get.evidence,
      first.anomalies,
      first.anomalies.postCancellationFills.head,
      link,
      MissingLineageState(LineageLinkLocation.Predecessor)
    )
    values.foreach(assertSerializationRejected)

end CancellationSuite
