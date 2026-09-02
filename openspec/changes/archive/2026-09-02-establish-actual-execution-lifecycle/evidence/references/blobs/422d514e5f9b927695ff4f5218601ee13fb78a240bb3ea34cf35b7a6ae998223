package external.execution.positive

import external.execution.fixtures.ExecutionLifecycleSetup.*
import trading.execution.*

object ExecutionAuthorityBoundaryClient:
  private def required[A](value: Either[?, A]): A =
    value.fold(error => throw new AssertionError(error.toString), identity)

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
