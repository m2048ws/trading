package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.order.Order
import trading.order.Side
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational

final class ExecutionStateSuite extends ScalaCheckSuite:
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
  private val sourceOrder = required(
    QualifiedSourceOrderId.create(target, id(NativeSourceOrderId.from("source-order")))
  )
  private val sourceStream = required(
    QualifiedSourceStreamId.create(target, id(SourceStreamId.from("orders")))
  )

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def executionTarget(source: String, account: String): ExecutionTarget =
    required(ExecutionTarget.create(id(ExecutionSourceId.from(source)), id(ExecutionAccountId.from(account))))

  private def event(value: String, executionTarget: ExecutionTarget = target): QualifiedSourceEventId =
    required(QualifiedSourceEventId.create(executionTarget, id(NativeSourceEventId.from(value))))

  private def fillId(value: String, executionTarget: ExecutionTarget = target): QualifiedFillId =
    required(QualifiedFillId.create(executionTarget, id(NativeFillId.from(value))))

  private def position(value: BigInt): QualifiedStreamPosition =
    required(QualifiedStreamPosition.create(sourceStream, id(SourceSequence.from(value))))

  private def ordering(value: BigInt, previous: Option[BigInt]): SourceOrdering =
    val at           = position(value)
    val continuation = previous match
      case None        => required(SourceContinuation.origin(sourceStream))
      case Some(value) => required(SourceContinuation.after(position(value)))
    required(SourceOrdering.sequenced(at, continuation))

  private def fill(
    eventValue: String,
    fillValue: String,
    lotCount: BigInt,
    sourceOrdering: SourceOrdering
  ): ExecutionFill[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] =
    required(
      ExecutionFill.create(lifecycle)(
        event(eventValue),
        lifecycle.executionOrderId,
        sourceOrder,
        fillId(fillValue),
        fixtures.lots(instrument, lotCount),
        fixtures.price(instrument, Rational.one),
        sourceOrdering
      )
    )

  private def initial: ExecutionState[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(ExecutionState.initial(lifecycle))

  private def accepted(
    transition: LifecycleTransition[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D
    ]
  ): LifecycleAccepted[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = transition match
    case value: LifecycleAccepted[?, ?, ?] => value.asInstanceOf[
        LifecycleAccepted[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D
        ]
      ]
    case value: LifecycleRejected[?, ?, ?] => fail(s"unexpected rejection: ${value.rejection}")

  private def diagnostics(
    state: ExecutionState[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D
    ]
  ): Vector[LifecycleDiagnostic] = state.observation.diagnostics.toVector.flatMap(_.toVector)

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("one immutable lifecycle state accepts commands, dispatch evidence, and source facts through total transitions"):
    val submit = required(
      SubmitOrderCommand.create(lifecycle)(id(ApplicationCommandId.from("submit")))
    )
    val commandTransition  = accepted(initial.record(submit))
    val dispatchTransition = accepted(
      commandTransition.state.observeDispatch(required(IndeterminateDispatch.forSubmit(submit)))
    )
    val fillTransition = accepted(
      dispatchTransition.state.record(fill("fill", "fill", 2, SourceOrdering.unsequenced))
    )

    assert(commandTransition.isInstanceOf[LifecycleApplied[?, ?, ?]])
    assert(dispatchTransition.isInstanceOf[LifecycleApplied[?, ?, ?]])
    assert(fillTransition.isInstanceOf[LifecycleApplied[?, ?, ?]])
    assertEquals(fillTransition.state.commands.issuedCommands.keySet, Set(submit.commandId))
    assertEquals(fillTransition.state.source.fillsById.size, 1)

    val foreignTarget    = executionTarget("other-source", "other-account")
    val foreignLifecycle = required(
      ExecutionLifecycle.create(instrument)(
        order,
        lifecycle.executionOrderId,
        lifecycle.lineageId,
        foreignTarget
      )
    )
    val foreignOrder = required(
      QualifiedSourceOrderId.create(foreignTarget, id(NativeSourceOrderId.from("source-order")))
    )
    val foreignFact = required(
      OrderAccepted.create(foreignLifecycle)(
        event("foreign", foreignTarget),
        lifecycle.executionOrderId,
        foreignOrder,
        SourceOrdering.unsequenced
      )
    )
    fillTransition.state.record(foreignFact) match
      case value: LifecycleRejected[?, ?, ?] =>
        assert(value.rejection.isInstanceOf[SourceInputRejected])
        assertEquals(value.state, fillTransition.state)
      case value => fail(s"foreign fact was accepted: $value")

  test("exact missing ranges close when late authoritative positions arrive and completeness is explicit"):
    val atZero  = fill("event-0", "fill-0", 1, ordering(0, None))
    val atThree = fill("event-3", "fill-3", 1, ordering(3, Some(0)))
    val gapped  = accepted(accepted(initial.record(atZero)).state.record(atThree)).state

    assertEquals(
      diagnostics(gapped).collect { case value: MissingSourceRange => value },
      Vector(
        MissingSourceRange(sourceStream, id(SourceSequence.from(1)), id(SourceSequence.from(2)))
      )
    )

    val atOne  = fill("event-1", "fill-1", 1, ordering(1, Some(0)))
    val atTwo  = fill("event-2", "fill-2", 1, ordering(2, Some(1)))
    val closed = accepted(accepted(gapped.record(atTwo)).state.record(atOne)).state
    assert(diagnostics(closed).forall(!_.isInstanceOf[MissingSourceRange]))
    assert(closed.observation.incompleteStreams.contains(sourceStream))
    assert(diagnostics(closed).contains(CompletenessNotEstablished(sourceStream)))

    val complete = required(
      SourceOrderCompleted.create(lifecycle)(
        event("complete"),
        lifecycle.executionOrderId,
        sourceOrder,
        required(SourceCompleteness.create(position(3))),
        SourceOrdering.unsequenced
      )
    )
    val completed = accepted(closed.record(complete)).state
    assertEquals(completed.observation.authoritativeCompleteness.keySet, Set(sourceStream))
    assert(!completed.observation.incompleteStreams.contains(sourceStream))

  test("continuation and checkpoint rewinds are explicit diagnostics independent of delivery time"):
    val atTwo = fill("event-2", "fill-2", 1, ordering(2, Some(3)))
    val state = accepted(initial.record(atTwo)).state
    assert(
      diagnostics(state).contains(
        SourceRewindObserved(sourceStream, id(SourceSequence.from(2)), id(SourceSequence.from(3)))
      )
    )

    val checkpoint = required(
      ReconciliationCheckpoint.create(lifecycle)(
        event("checkpoint"),
        lifecycle.executionOrderId,
        sourceOrder,
        required(SourceCheckpoint.create(position(1), required(SourceContinuation.origin(sourceStream)))),
        SourceOrdering.unsequenced
      )
    )
    val rewound = accepted(state.record(checkpoint)).state
    assert(
      diagnostics(rewound).contains(
        SourceRewindObserved(sourceStream, id(SourceSequence.from(1)), id(SourceSequence.from(2)))
      )
    )

  test("conflicting stream claimants remain retained and make the stream incomplete"):
    val samePosition = ordering(0, None)
    val first        = fill("event-a", "fill-a", 1, samePosition)
    val second       = fill("event-b", "fill-b", 1, samePosition)
    val state        = accepted(accepted(initial.record(first)).state.record(second)).state
    val conflict     = diagnostics(state).collectFirst:
      case value: StreamPositionConflictObserved => value

    assertEquals(conflict, Some(StreamPositionConflictObserved(position(0), 2)))
    assert(state.observation.incompleteStreams.contains(sourceStream))

  test("unresolved modifier authority clears when the referenced fill arrives without deleting history"):
    val targetFill = fill("fill", "fill", 2, SourceOrdering.unsequenced)
    val bust       = required(
      FillBusted.create(lifecycle)(
        event("bust"),
        lifecycle.executionOrderId,
        sourceOrder,
        targetFill.fillId,
        SourceOrdering.unsequenced
      )
    )
    val unresolved = accepted(initial.record(bust)).state
    assert(diagnostics(unresolved).contains(UnresolvedFillObserved(targetFill.fillId, bust.eventId)))

    val resolved = accepted(unresolved.record(targetFill)).state
    assert(!resolved.observation.unresolvedFillReferences.contains(targetFill.fillId))
    assertEquals(resolved.observation.sourceFacts.keySet, Set(bust.eventId, targetFill.eventId))

  test("explicitly unsequenced facts remain identity-usable without inferred before-or-after semantics"):
    val first  = fill("z-event", "fill-z", 1, SourceOrdering.unsequenced)
    val second = required(
      OrderAccepted.create(lifecycle)(
        event("a-event"),
        lifecycle.executionOrderId,
        sourceOrder,
        SourceOrdering.unsequenced
      )
    )
    val state = accepted(accepted(initial.record(first)).state.record(second)).state
    assertEquals(
      state.observation.explicitlyUnsequencedEvents,
      Vector(second.eventId, first.eventId)
    )
    assert(state.observation.incompleteStreams.isEmpty)

  property("canonical replay converges for authority-equivalent delivery permutations"):
    forAll { (reverse: Boolean) =>
      val facts = Vector(
        fill("event-0", "fill-0", 1, ordering(0, None)),
        fill("event-1", "fill-1", 1, ordering(1, Some(0))),
        fill("event-2", "fill-2", 1, ordering(2, Some(1)))
      )
      val first  = required(ExecutionState.replay(lifecycle)(Vector.empty, Vector.empty, facts))
      val input  = if reverse then facts.reverse else facts
      val second = required(ExecutionState.replay(lifecycle)(Vector.empty, Vector.empty, input))

      first == second && first.state.observation == second.state.observation && first.rejections.isEmpty
    }

  test("canonical replay gives conflicts a stable structural order"):
    val eventConflict = Vector(
      fill("same-event", "fill-b", 2, ordering(0, None)),
      fill("same-event", "fill-a", 1, ordering(0, None))
    )
    val positionConflict = Vector(
      fill("event-d", "fill-d", 1, ordering(1, Some(0))),
      fill("event-c", "fill-c", 1, ordering(1, Some(0)))
    )
    val facts   = eventConflict ++ positionConflict
    val forward = required(ExecutionState.replay(lifecycle)(Vector.empty, Vector.empty, facts))
    val reverse = required(ExecutionState.replay(lifecycle)(Vector.empty, Vector.empty, facts.reverse))

    assertEquals(forward, reverse)
    assertEquals(forward.state.observation, reverse.state.observation)
    assert(diagnostics(forward.state).exists(_.isInstanceOf[SourceEventConflictObserved]))
    assert(diagnostics(forward.state).exists(_.isInstanceOf[StreamPositionConflictObserved]))
    assertEquals(forward.state.observation.sourceEventConflicts.size, 1)
    assertEquals(forward.state.observation.streamPositionConflicts.keySet, Set(position(1)))

  test("canonical replay sends missing elements through checked rejection transitions"):
    val replay = required(
      ExecutionState.replay(lifecycle)(
        Vector(null.asInstanceOf[ExecutionCommand[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D
        ]]),
        Vector(null.asInstanceOf[DispatchEvidence[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D
        ]]),
        Vector(null.asInstanceOf[SourceFact[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D
        ]])
      )
    )

    assertEquals(replay.rejections.size, 3)
    assertEquals(replay.state, initial)

  test("ordinary indexed updates and duplicates report no full-history scan"):
    val original = fill("event", "fill", 1, SourceOrdering.unsequenced)
    val applied  = accepted(initial.record(original))
    val replayed = accepted(applied.state.record(original))

    assertEquals(applied.work, TransitionWork(2, 2, 0))
    assert(replayed.isInstanceOf[LifecycleIdempotent[?, ?, ?]])
    assertEquals(replayed.work, TransitionWork(2, 0, 0))
    assertEquals(replayed.state, applied.state)

  test("lifecycle state, transitions, observations, and replay results reject Java serialization"):
    val original    = fill("event", "fill", 1, ordering(2, None))
    val transition  = accepted(initial.record(original))
    val replay      = required(ExecutionState.replay(lifecycle)(Vector.empty, Vector.empty, Vector(original)))
    val observation = transition.state.observation

    val values: List[JavaSerializationUnsupported] = List(
      transition.state,
      transition,
      observation,
      replay,
      observation.diagnostics.get,
      TransitionWork(1, 1, 0),
      MissingSourceRange(sourceStream, id(SourceSequence.from(1)), id(SourceSequence.from(2)))
    )
    values.foreach(assertSerializationRejected)

end ExecutionStateSuite
