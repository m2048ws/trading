package external.execution.positive

import external.execution.fixtures.ExecutionLifecycleSetup.*
import trading.execution.*
import trading.quantity.Dim

object ExecutionAuthorityBoundaryClient:
  private def required[A](value: Either[?, A]): A =
    value.fold(error => throw new AssertionError(error.toString), identity)

  private def acceptedTransition[D <: Dim, B <: Dim, Q <: Dim](
    value: LifecycleTransition[D, B, Q]
  ): LifecycleAccepted[D, B, Q] = value match
    case transition: LifecycleAccepted[?, ?, ?] =>
      transition.asInstanceOf[LifecycleAccepted[D, B, Q]]
    case transition: LifecycleRejected[?, ?, ?] =>
      throw new AssertionError(transition.rejection.toString)

  def run(): Unit =
    val target = required(
      ExecutionTarget.create(
        required(ExecutionSourceId.from("source")),
        required(ExecutionAccountId.from("account"))
      )
    )
    val lifecycle = required(
      ExecutionLifecycle.create(instrument)(
        order,
        required(ExecutionOrderId.from("logical-order")),
        required(OrderLineageId.from("lineage")),
        target
      )
    )
    val stream = required(
      QualifiedSourceStreamId.create(target, required(SourceStreamId.from("stream")))
    )
    val position = required(
      QualifiedStreamPosition.create(stream, required(SourceSequence.from(BigInt(3))))
    )
    val ordering = required(
      SourceOrdering.sequenced(position, required(SourceContinuation.origin(stream)))
    )

    assert(lifecycle.instrumentId == instrument.identity.id)
    assert(lifecycle.orderedLots == lots)
    assert(ordering.position == position)

    val submit = required(
      SubmitOrderCommand.create(lifecycle)(required(ApplicationCommandId.from("submit")))
    )
    val cancel = required(
      CancelOrderCommand.create(lifecycle)(required(ApplicationCommandId.from("cancel")), submit.commandId)
    )
    val submitted = required(CommandState.initial(lifecycle)).record(submit)
    val cancelled = submitted.state.record(cancel)
    val observed = cancelled.state.observeDispatch(
      required(IndeterminateDispatch.forSubmit(submit))
    )

    val commandKinds = observed.state.issuedCommands.values.toVector.map:
      case _: SubmitOrderCommand[?, ?, ?] => "submit"
      case _: CancelOrderCommand[?, ?, ?] => "cancel"
    val dispatchKinds = observed.state.dispatchKnowledge(submit.commandId).map:
      case _: ProvenNotDispatched[?, ?, ?] => "proven-not-dispatched"
      case _: IndeterminateDispatch[?, ?, ?] => "indeterminate"

    assert(commandKinds.toSet == Set("submit", "cancel"))
    assert(dispatchKinds == Vector("indeterminate"))
    assert(cancelled.kind == CommandTransitionKind.Applied)
    assert(observed.kind == CommandTransitionKind.Applied)

    val eventId = required(
      QualifiedSourceEventId.create(target, required(NativeSourceEventId.from("accepted")))
    )
    val sourceOrderId = required(
      QualifiedSourceOrderId.create(target, required(NativeSourceOrderId.from("source-order")))
    )
    val fillId = required(
      QualifiedFillId.create(target, required(NativeFillId.from("fill")))
    )
    val accepted = required(
      OrderAccepted.create(lifecycle)(
        eventId,
        lifecycle.executionOrderId,
        sourceOrderId,
        ordering
      )
    )
    val executionFill = required(
      ExecutionFill.create(lifecycle)(
        required(QualifiedSourceEventId.create(target, required(NativeSourceEventId.from("fill")))),
        lifecycle.executionOrderId,
        sourceOrderId,
        fillId,
        lots,
        price,
        SourceOrdering.unsequenced
      )
    )
    val correction = required(
      FillCorrected.create(lifecycle)(
        required(QualifiedSourceEventId.create(target, required(NativeSourceEventId.from("correction")))),
        lifecycle.executionOrderId,
        sourceOrderId,
        fillId,
        lots,
        price,
        SourceOrdering.unsequenced
      )
    )
    val sourceState = required(SourceEvidenceState.initial(lifecycle))
    val unresolved  = sourceState.record(correction).state
    val resolved    = unresolved.record(executionFill).state

    val factKinds = Vector[SourceFact[?, ?, ?]](accepted, executionFill, correction).map:
      case _: OrderAccepted[?, ?, ?]              => "accepted"
      case _: OrderRejected[?, ?, ?]              => "rejected"
      case _: ExecutionFill[?, ?, ?]              => "fill"
      case _: FillCorrected[?, ?, ?]              => "corrected"
      case _: FillBusted[?, ?, ?]                 => "busted"
      case _: CancellationEffective[?, ?, ?]      => "cancelled"
      case _: ReconciliationCheckpoint[?, ?, ?]   => "checkpoint"
      case _: SourceOrderCompleted[?, ?, ?]       => "complete"
      case _: SourceOrderAbsent[?, ?, ?]          => "absent"

    assert(factKinds == Vector("accepted", "fill", "corrected"))
    assert(unresolved.unresolvedFillReferences.contains(fillId))
    assert(!resolved.unresolvedFillReferences.contains(fillId))
    assert(resolved.fillsById(fillId).price == price)

    val commandApplied = acceptedTransition(required(ExecutionState.initial(lifecycle)).record(submit))
    val cancelApplied  = acceptedTransition(commandApplied.state.record(cancel))
    val fillApplied    = acceptedTransition(cancelApplied.state.record(executionFill))
    val confirmedCancellation = required(
      CancellationEffective.create(lifecycle)(
        required(QualifiedSourceEventId.create(target, required(NativeSourceEventId.from("cancelled")))),
        lifecycle.executionOrderId,
        sourceOrderId,
        ordering
      )
    )
    val cancellationApplied = acceptedTransition(fillApplied.state.record(confirmedCancellation))
    val observation         = cancellationApplied.state.observation
    val replay         = required(
      ExecutionState.replay(lifecycle)(Vector(submit, cancel), Vector.empty, Vector(executionFill, confirmedCancellation))
    )
    val transitionKinds = Vector[LifecycleTransition[?, ?, ?]](
      commandApplied,
      cancelApplied,
      fillApplied,
      cancellationApplied
    ).map:
      case value: LifecycleAccepted[?, ?, ?] => value.kind
      case _: LifecycleRejected[?, ?, ?]     => throw new AssertionError("unexpected lifecycle rejection")

    assert(transitionKinds == Vector.fill(4)(LifecycleTransitionKind.Applied))
    assert(observation.issuedCommands.keySet == Set(submit.commandId, cancel.commandId))
    assert(observation.fills.keySet == Set(fillId))
    assert(observation.commandConflicts.isEmpty)
    assert(observation.sourceEventConflicts.isEmpty)
    assert(observation.fillIdentityConflicts.isEmpty)
    assert(observation.streamPositionConflicts.isEmpty)
    assert(observation.explicitlyUnsequencedEvents == Vector(executionFill.eventId))
    val effectiveFillKind = observation.effectiveFillLedger.byFillId(fillId) match
      case _: ActiveEffectiveFill[?, ?, ?]      => "active"
      case _: BustedEffectiveFill[?, ?, ?]      => "busted"
      case _: AmbiguousEffectiveFill[?, ?, ?]   => "ambiguous"
      case _: ConflictingEffectiveFill[?, ?, ?] => "conflicting"
    assert(effectiveFillKind == "active")
    assert(observation.effectiveFillLedger.knownExposure == lifecycle.initialPositionChange)
    assert(observation.effectiveFillLedger.overfill.isEmpty)
    val cancellationKind = observation.cancellationKnowledge.get match
      case _: CancellationRequested[?, ?, ?]  => "requested"
      case _: CancellationConfirmed[?, ?, ?]  => "confirmed"
      case _: CancellationConflicted[?, ?, ?] => "conflicted"
    assert(cancellationKind == "confirmed")
    assert(observation.anomalies.isEmpty)
    val submissionKind = observation.submissionKnowledge.get match
      case _: IssuedPendingSubmission[?, ?, ?]         => "pending"
      case _: AcceptedSubmission[?, ?, ?]              => "accepted"
      case _: RejectedSubmission[?, ?, ?]              => "rejected"
      case _: ProvenNotDispatchedSubmission[?, ?, ?]   => "not-dispatched"
      case _: IndeterminateSubmission[?, ?, ?]         => "indeterminate"
      case _: ExecutionProvenSubmission[?, ?, ?]       => "execution-proven"
      case _: AuthoritativelyAbsentSubmission[?, ?, ?] => "absent"
      case _: ConflictingSubmission[?, ?, ?]           => "conflicting"
    assert(submissionKind == "execution-proven")
    val successorLifecycle = required(
      ExecutionLifecycle.create(instrument)(
        order,
        required(ExecutionOrderId.from("successor-order")),
        lifecycle.lineageId,
        target
      )
    )
    val successorSubmit = required(
      SubmitOrderCommand.create(successorLifecycle)(required(ApplicationCommandId.from("successor-submit")))
    )
    val successorState = acceptedTransition(
      required(ExecutionState.initial(successorLifecycle)).record(successorSubmit)
    ).state
    val lineage = required(OrderLineageLink.create(cancellationApplied.state, successorState))
    assert(lineage.predecessorExecutionOrderId == lifecycle.executionOrderId)
    assert(lineage.successorExecutionOrderId == successorLifecycle.executionOrderId)
    assert(lineage.lineageId == lifecycle.lineageId)
    assert(replay.state == cancellationApplied.state)
    assert(replay.rejections.isEmpty)
