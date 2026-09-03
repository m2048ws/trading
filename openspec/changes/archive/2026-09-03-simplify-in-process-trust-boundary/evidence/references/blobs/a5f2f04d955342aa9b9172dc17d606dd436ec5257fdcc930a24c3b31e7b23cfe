package trading.execution

import trading.economics.instrument.InstrumentId
import trading.quantity.JavaSerializationUnsupported

enum LineageLinkLocation extends JavaSerializationUnsupported:
  case Predecessor
  case Successor

sealed abstract class LineageLinkViolation extends JavaSerializationUnsupported with Product with Serializable

final case class MissingLineageState(location: LineageLinkLocation) extends LineageLinkViolation

final case class SameLineageExecutionOrder(executionOrderId: ExecutionOrderId) extends LineageLinkViolation

final case class LineageIdentityMismatch(expected: OrderLineageId, supplied: OrderLineageId)
  extends LineageLinkViolation

final case class LineageInstrumentMismatch(expected: InstrumentId, supplied: InstrumentId) extends LineageLinkViolation

final case class LineageInstrumentDefinitionMismatch(instrumentId: InstrumentId) extends LineageLinkViolation

final case class PredecessorCancellationNotConfirmed(executionOrderId: ExecutionOrderId) extends LineageLinkViolation

final case class SuccessorSubmissionNotRecorded(executionOrderId: ExecutionOrderId) extends LineageLinkViolation

final class LineageLinkViolations private[execution] (private val values: Vector[LineageLinkViolation])
  extends JavaSerializationUnsupported:

  def head: LineageLinkViolation             = values.head
  def toVector: Vector[LineageLinkViolation] = values
  def size: Int                              = values.size

  override def equals(other: Any): Boolean = other match
    case that: LineageLinkViolations => values == that.toVector
    case _                           => false
  override def hashCode(): Int = values.hashCode

final class OrderLineageLink private (
  val lineageId: OrderLineageId,
  val predecessor: ExecutionLifecycle[?, ?, ?],
  val successor: ExecutionLifecycle[?, ?, ?],
  val predecessorCancellation: CancellationConfirmed[?, ?, ?],
  val successorSubmissions: Set[SubmitOrderCommand[?, ?, ?]])
  extends JavaSerializationUnsupported:

  val predecessorExecutionOrderId: ExecutionOrderId = predecessor.executionOrderId
  val successorExecutionOrderId: ExecutionOrderId   = successor.executionOrderId

  override def equals(other: Any): Boolean = other match
    case that: OrderLineageLink =>
      lineageId == that.lineageId && predecessor == that.predecessor && successor == that.successor &&
      predecessorCancellation == that.predecessorCancellation && successorSubmissions == that.successorSubmissions
    case _ => false
  override def hashCode(): Int =
    (lineageId, predecessor, successor, predecessorCancellation, successorSubmissions).hashCode
end OrderLineageLink

object OrderLineageLink:
  private def violations(values: Vector[LineageLinkViolation]): LineageLinkViolations =
    new LineageLinkViolations(values)

  private def construct(
    lineageId: OrderLineageId,
    predecessor: ExecutionLifecycle[?, ?, ?],
    successor: ExecutionLifecycle[?, ?, ?],
    predecessorCancellation: CancellationConfirmed[?, ?, ?],
    successorSubmissions: Set[SubmitOrderCommand[?, ?, ?]]
  ): OrderLineageLink =
    new OrderLineageLink(lineageId, predecessor, successor, predecessorCancellation, successorSubmissions)

  def create(
    predecessorState: ExecutionState[?, ?, ?],
    successorState: ExecutionState[?, ?, ?]
  ): Either[LineageLinkViolations, OrderLineageLink] =
    val missing = Vector(
      Option.when(predecessorState == null)(MissingLineageState(LineageLinkLocation.Predecessor)),
      Option.when(successorState == null)(MissingLineageState(LineageLinkLocation.Successor))
    ).flatten
    if missing.nonEmpty then Left(violations(missing))
    else
      val predecessor  = predecessorState.lifecycle
      val successor    = successorState.lifecycle
      val cancellation = CancellationKnowledge.derive(predecessorState).collect:
        case value: CancellationConfirmed[?, ?, ?] => value
      val submissions: Set[SubmitOrderCommand[?, ?, ?]] =
        successorState.commands.issuedCommands.values.collect:
          case value: SubmitOrderCommand[?, ?, ?] => value: SubmitOrderCommand[?, ?, ?]
        .toSet
      val invalid = Vector(
        Option.when(predecessor.executionOrderId == successor.executionOrderId)(
          SameLineageExecutionOrder(predecessor.executionOrderId)
        ),
        Option.when(predecessor.lineageId != successor.lineageId)(
          LineageIdentityMismatch(predecessor.lineageId, successor.lineageId)
        ),
        Option.when(predecessor.instrumentId != successor.instrumentId)(
          LineageInstrumentMismatch(predecessor.instrumentId, successor.instrumentId)
        ),
        Option.when(
          predecessor.instrumentId == successor.instrumentId &&
            !LifecycleValueSemantics.sameInstrument(predecessor.instrument, successor.instrument)
        )(
          LineageInstrumentDefinitionMismatch(predecessor.instrumentId)
        ),
        Option.when(cancellation.isEmpty)(
          PredecessorCancellationNotConfirmed(predecessor.executionOrderId)
        ),
        Option.when(submissions.isEmpty)(SuccessorSubmissionNotRecorded(successor.executionOrderId))
      ).flatten
      if invalid.nonEmpty then Left(violations(invalid))
      else
        Right(
          construct(
            predecessor.lineageId,
            predecessor,
            successor,
            cancellation.get,
            submissions
          )
        )
    end if
  end create
end OrderLineageLink
