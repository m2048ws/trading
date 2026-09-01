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
  val copied = checkedTarget.copy(source = null)
  final class ForgedContinuation extends SourceContinuation()
  final class ForgedOrdering extends SourceOrdering()
  // OFFENDING-END
