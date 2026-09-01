package trading.execution

import trading.quantity.Dim

object PackageSpoofExecutionAuthority:
  val baseline = "checked factories only"
  val checkedTarget = ExecutionTarget
    .create(
      ExecutionSourceId.from("source").toOption.get,
      ExecutionAccountId.from("account").toOption.get
    )
    .toOption
    .get

  // OFFENDING-BEGIN
  val target = new ExecutionTarget(null, null)
  val event = new QualifiedSourceEventId(null, null)
  val stream = new QualifiedSourceStreamId(null, null)
  val position = new QualifiedStreamPosition(null, null)
  val sequenced = new AuthoritativelySequenced(null, null)
  val checkpoint = new SourceCheckpoint(null, null)
  val completeness = new SourceCompleteness(null)
  val lifecycle = new ExecutionLifecycle[Dim, Dim, Dim](null, null, null, null, null, null)
  val commandErrors = new CommandViolations(Vector.empty)
  val submit = new SubmitOrderCommand[Dim, Dim, Dim](null, null)
  val cancel = new CancelOrderCommand[Dim, Dim, Dim](null, null, null)
  val notDispatched = new ProvenNotDispatched[Dim, Dim, Dim](null)
  val indeterminate = new IndeterminateDispatch[Dim, Dim, Dim](null)
  val conflict = new CommandConflict[Dim, Dim, Dim](null, null)
  val commandState = new CommandState[Dim, Dim, Dim](null, Map.empty, Map.empty, Vector.empty, Vector.empty)
  val transition = new CommandTransition[Dim, Dim, Dim](null, null, None)
  val copied = checkedTarget.copy(source = null)
  final class ForgedContinuation extends SourceContinuation()
  final class ForgedOrdering extends SourceOrdering()
  final class ForgedCommand extends ExecutionCommand[Dim, Dim, Dim]():
    val commandId = null
    val lifecycle = null
  final class ForgedDispatch extends DispatchEvidence[Dim, Dim, Dim]():
    val submit = null
  val nativeAmend = null.asInstanceOf[SubmitOrderCommand[Dim, Dim, Dim]].amend(null)
  val atomicReplace = null.asInstanceOf[CommandState[Dim, Dim, Dim]].cancelReplace(null)
  // OFFENDING-END
