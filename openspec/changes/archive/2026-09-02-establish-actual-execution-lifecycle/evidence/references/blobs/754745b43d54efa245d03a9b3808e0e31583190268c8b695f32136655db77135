package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.lang.reflect.Modifier

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.order.Order
import trading.order.Side
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational

final class SubmissionKnowledgeSuite extends ScalaCheckSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear
  private val target     = required(
    ExecutionTarget.create(id(ExecutionSourceId.from("source")), id(ExecutionAccountId.from("account")))
  )
  private val order     = Order.market(instrument)(Side.Buy, fixtures.lots(instrument, 10)).toOption.get
  private val lifecycle = required(
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
    QualifiedSourceStreamId.create(target, id(SourceStreamId.from("lookup")))
  )

  private type State = ExecutionState[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ]
  private type Transition = LifecycleTransition[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ]

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def event(value: String): QualifiedSourceEventId =
    required(QualifiedSourceEventId.create(target, id(NativeSourceEventId.from(value))))

  private def position(value: BigInt): QualifiedStreamPosition =
    required(QualifiedStreamPosition.create(sourceStream, id(SourceSequence.from(value))))

  private def ordering(value: BigInt, previous: Option[BigInt]): SourceOrdering =
    val at           = position(value)
    val continuation = previous match
      case None        => required(SourceContinuation.origin(sourceStream))
      case Some(value) => required(SourceContinuation.after(position(value)))
    required(SourceOrdering.sequenced(at, continuation))

  private def initial: State = required(ExecutionState.initial(lifecycle))

  private def submit(value: String = "submit"): SubmitOrderCommand[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(SubmitOrderCommand.create(lifecycle)(id(ApplicationCommandId.from(value))))

  private def accepted(transition: Transition): LifecycleAccepted[
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

  private def rejected(transition: Transition): LifecycleRejected[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = transition match
    case value: LifecycleRejected[?, ?, ?] => value.asInstanceOf[
        LifecycleRejected[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D
        ]
      ]
    case value: LifecycleAccepted[?, ?, ?] => fail(s"unexpected acceptance: ${value.kind}")

  private def issued(
    command: SubmitOrderCommand[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D
    ]
  ): State = accepted(initial.record(command)).state

  private def acceptance(value: String = "accepted"): OrderAccepted[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(
    OrderAccepted.create(lifecycle)(
      event(value),
      lifecycle.executionOrderId,
      sourceOrder,
      SourceOrdering.unsequenced
    )
  )

  private def rejection(value: String = "rejected"): OrderRejected[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(
    OrderRejected.create(lifecycle)(
      event(value),
      lifecycle.executionOrderId,
      sourceOrder,
      SourceOrdering.unsequenced
    )
  )

  private def fill(value: String = "fill"): ExecutionFill[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(
    ExecutionFill.create(lifecycle)(
      event(value),
      lifecycle.executionOrderId,
      sourceOrder,
      required(QualifiedFillId.create(target, id(NativeFillId.from(value)))),
      fixtures.lots(instrument, 2),
      fixtures.price(instrument, Rational.one),
      SourceOrdering.unsequenced
    )
  )

  private def absence(authoritative: Boolean): SourceOrderAbsent[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] =
    val completeThrough = position(0)
    required(
      SourceOrderAbsent.create(lifecycle)(
        event(if authoritative then "authoritative-absence" else "unproven-absence"),
        lifecycle.executionOrderId,
        sourceOrder,
        required(SourceCompleteness.create(completeThrough)),
        if authoritative then ordering(0, None) else SourceOrdering.unsequenced
      )
    )

  private def knowledge(state: State): SubmissionKnowledge[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = state.observation.submissionKnowledge.getOrElse(fail("expected submission knowledge"))

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("submission knowledge exposes every closed epistemic alternative with supporting evidence"):
    val command       = submit()
    val pending       = issued(command)
    val acceptedState = accepted(pending.record(acceptance())).state
    val rejectedState = accepted(pending.record(rejection())).state
    val notDispatched = accepted(
      pending.observeDispatch(required(ProvenNotDispatched.forSubmit(command)))
    ).state
    val indeterminate = accepted(
      pending.observeDispatch(required(IndeterminateDispatch.forSubmit(command)))
    ).state
    val executed = accepted(indeterminate.record(fill())).state
    val absent   = accepted(pending.record(absence(authoritative = true))).state
    val conflict = accepted(notDispatched.record(acceptance("accepted-after-proof"))).state

    val alternatives = Vector(
      knowledge(pending),
      knowledge(acceptedState),
      knowledge(rejectedState),
      knowledge(notDispatched),
      knowledge(indeterminate),
      knowledge(executed),
      knowledge(absent),
      knowledge(conflict)
    )
    val kinds = alternatives.map:
      case _: IssuedPendingSubmission[?, ?, ?]         => "pending"
      case _: AcceptedSubmission[?, ?, ?]              => "accepted"
      case _: RejectedSubmission[?, ?, ?]              => "rejected"
      case _: ProvenNotDispatchedSubmission[?, ?, ?]   => "not-dispatched"
      case _: IndeterminateSubmission[?, ?, ?]         => "indeterminate"
      case _: ExecutionProvenSubmission[?, ?, ?]       => "execution-proven"
      case _: AuthoritativelyAbsentSubmission[?, ?, ?] => "absent"
      case _: ConflictingSubmission[?, ?, ?]           => "conflicting"

    assertEquals(
      kinds,
      Vector("pending", "accepted", "rejected", "not-dispatched", "indeterminate", "execution-proven", "absent",
        "conflicting")
    )
    assertEquals(initial.observation.submissionKnowledge, None)
    assertEquals(knowledge(acceptedState).evidence.submitCommands, Set(command))
    assertEquals(knowledge(executed).evidence.executionFills, Set(fill()))

  test("indeterminate dispatch blocks a fresh submit while exact recovery and defensive cancellation remain allowed"):
    val original  = submit()
    val uncertain = accepted(
      issued(original).observeDispatch(required(IndeterminateDispatch.forSubmit(original)))
    ).state
    val fresh   = submit("fresh-submit")
    val blocked = rejected(uncertain.record(fresh))
    val retried = accepted(uncertain.record(original))
    val cancel  = required(
      CancelOrderCommand.create(lifecycle)(id(ApplicationCommandId.from("cancel")), original.commandId)
    )
    val defensive = accepted(uncertain.record(cancel))
    val replay    = required(
      ExecutionState.replay(lifecycle)(
        Vector(fresh, original),
        Vector(required(IndeterminateDispatch.forSubmit(original))),
        Vector.empty
      )
    )

    assertEquals(blocked.state, uncertain)
    assertEquals(
      blocked.rejection,
      CommandInputRejected(
        CommandViolations.one(FreshSubmitBlockedByIndeterminate(original.commandId, fresh.commandId))
      )
    )
    assertEquals(retried.kind, LifecycleTransitionKind.IdempotentDuplicate)
    assertEquals(defensive.kind, LifecycleTransitionKind.Applied)
    assertEquals(defensive.state.commands.cancellationRequests, Vector(cancel))
    assertEquals(replay.state.commands.issuedCommands.keySet, Set(original.commandId))
    assertEquals(
      replay.rejections,
      Vector(
        CommandInputRejected(
          CommandViolations.one(FreshSubmitBlockedByIndeterminate(original.commandId, fresh.commandId))
        )
      )
    )

  test("later acceptance and rejection refine uncertainty without deleting dispatch evidence"):
    val command   = submit()
    val uncertain = accepted(
      issued(command).observeDispatch(required(IndeterminateDispatch.forSubmit(command)))
    ).state
    val acceptedKnowledge = knowledge(accepted(uncertain.record(acceptance())).state)
    val rejectedKnowledge = knowledge(accepted(uncertain.record(rejection())).state)

    assert(acceptedKnowledge.isInstanceOf[AcceptedSubmission[?, ?, ?]])
    assert(rejectedKnowledge.isInstanceOf[RejectedSubmission[?, ?, ?]])
    assertEquals(acceptedKnowledge.evidence.dispatchEvidence.size, 1)
    assertEquals(rejectedKnowledge.evidence.dispatchEvidence.size, 1)

  test("a fill proves execution without fabricating acceptance"):
    val command   = submit()
    val uncertain = accepted(
      issued(command).observeDispatch(required(IndeterminateDispatch.forSubmit(command)))
    ).state
    val execution = knowledge(accepted(uncertain.record(fill())).state)

    assert(execution.isInstanceOf[ExecutionProvenSubmission[?, ?, ?]])
    assert(execution.evidence.acceptances.isEmpty)
    assertEquals(execution.evidence.executionFills.size, 1)

  test("absence proves non-acceptance only when its completeness stream is authoritative"):
    val command       = submit()
    val pending       = issued(command)
    val unproven      = accepted(pending.record(absence(authoritative = false))).state
    val authoritative = accepted(pending.record(absence(authoritative = true))).state

    assert(knowledge(unproven).isInstanceOf[IssuedPendingSubmission[?, ?, ?]])
    assertEquals(knowledge(unproven).evidence.sourceAbsences.size, 1)
    assert(knowledge(authoritative).isInstanceOf[AuthoritativelyAbsentSubmission[?, ?, ?]])
    assertEquals(authoritative.observation.authoritativeCompleteness.keySet, Set(sourceStream))

  test("externally contradictory outcomes remain a deterministic non-empty conflict"):
    val command = submit()
    val proven  = accepted(
      issued(command).observeDispatch(required(ProvenNotDispatched.forSubmit(command)))
    ).state
    val withAcceptance = accepted(proven.record(acceptance())).state
    val withRejection  = accepted(withAcceptance.record(rejection())).state
    val value          = knowledge(withRejection) match
      case conflict: ConflictingSubmission[?, ?, ?] => conflict
      case other                                    => fail(s"expected conflict, received $other")

    assertEquals(
      value.conflicts.toVector,
      Vector(
        SubmissionConflictKind.ProvenNonDispatchVersusSourceOutcome,
        SubmissionConflictKind.AcceptanceVersusRejection
      )
    )
    assertEquals(value.evidence.acceptances.size, 1)
    assertEquals(value.evidence.rejections.size, 1)

  property("same-command recovery and authority-equivalent fact permutations are deterministic"):
    forAll { (reverse: Boolean) =>
      val command = submit()
      val facts   = Vector[SourceFact[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D
      ]](acceptance(), fill())
      val input = if reverse then facts.reverse else facts
      val first = required(
        ExecutionState.replay(lifecycle)(
          Vector(command, command),
          Vector(required(IndeterminateDispatch.forSubmit(command))),
          facts
        )
      )
      val second = required(
        ExecutionState.replay(lifecycle)(
          Vector(command, command),
          Vector(required(IndeterminateDispatch.forSubmit(command))),
          input
        )
      )

      first == second && first.state.observation == second.state.observation &&
      knowledge(first.state).isInstanceOf[AcceptedSubmission[?, ?, ?]]
    }

  test("submission evidence and closed knowledge representations are guarded immutable values"):
    val command         = submit()
    val value           = knowledge(issued(command))
    val representations = List(
      classOf[SubmissionEvidence[?, ?, ?]],
      classOf[SubmissionConflicts],
      classOf[IssuedPendingSubmission[?, ?, ?]],
      classOf[AcceptedSubmission[?, ?, ?]],
      classOf[RejectedSubmission[?, ?, ?]],
      classOf[ProvenNotDispatchedSubmission[?, ?, ?]],
      classOf[IndeterminateSubmission[?, ?, ?]],
      classOf[ExecutionProvenSubmission[?, ?, ?]],
      classOf[AuthoritativelyAbsentSubmission[?, ?, ?]],
      classOf[ConflictingSubmission[?, ?, ?]]
    )
    representations.foreach: representation =>
      assert(Modifier.isFinal(representation.getModifiers), s"${representation.getName} must be final")
      assert(
        representation.getDeclaredConstructors.forall(constructor => Modifier.isPrivate(constructor.getModifiers)),
        s"${representation.getName} exposes a non-private JVM constructor"
      )

    List[JavaSerializationUnsupported](value, value.evidence).foreach(assertSerializationRejected)

end SubmissionKnowledgeSuite
