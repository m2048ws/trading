package trading.execution

import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

enum CommandViolationLocation extends JavaSerializationUnsupported:
  case Lifecycle
  case Command
  case CommandIdentity
  case LogicalExecutionOrder
  case Lineage
  case Target
  case ImmutableOrder
  case OriginalSubmit
  case DispatchEvidence

sealed abstract class CommandViolation extends JavaSerializationUnsupported with Product with Serializable

final case class MissingCommandValue(location: CommandViolationLocation) extends CommandViolation

final case class CommandLogicalOrderMismatch(
  expected: ExecutionOrderId,
  supplied: ExecutionOrderId)
  extends CommandViolation

final case class CommandLineageMismatch(expected: OrderLineageId, supplied: OrderLineageId) extends CommandViolation

final case class CommandTargetMismatch(expected: ExecutionTarget, supplied: ExecutionTarget) extends CommandViolation

final case class CommandImmutableOrderMismatch(commandId: ApplicationCommandId) extends CommandViolation

final case class UnknownOriginalSubmit(commandId: ApplicationCommandId) extends CommandViolation

final case class ReferencedCommandIsNotSubmit(commandId: ApplicationCommandId) extends CommandViolation

final case class DispatchSubmitBodyMismatch(commandId: ApplicationCommandId) extends CommandViolation

final case class FreshSubmitBlockedByIndeterminate(
  existingCommandId: ApplicationCommandId,
  attemptedCommandId: ApplicationCommandId)
  extends CommandViolation
final class CommandViolations private (private val values: Vector[CommandViolation])
  extends JavaSerializationUnsupported:

  def head: CommandViolation             = values.head
  def toVector: Vector[CommandViolation] = values
  def size: Int                          = values.size

  override def equals(other: Any): Boolean = other match
    case that: CommandViolations => values == that.toVector
    case _                       => false

  override def hashCode(): Int  = values.hashCode
  override def toString: String = values.mkString("CommandViolations(", ",", ")")

object CommandViolations:
  private def construct(values: Vector[CommandViolation]): CommandViolations =
    new CommandViolations(values)

  def one(value: CommandViolation): CommandViolations = construct(Vector(value))

  private[execution] def from(values: Vector[CommandViolation]): Option[CommandViolations] =
    Option.when(values.nonEmpty)(construct(values))

sealed abstract class ExecutionCommand[D <: Dim, B <: Dim, Q <: Dim] protected () extends JavaSerializationUnsupported:
  def commandId: ApplicationCommandId
  def lifecycle: ExecutionLifecycle[D, B, Q]

  final def executionOrderId: ExecutionOrderId = lifecycle.executionOrderId
  final def lineageId: OrderLineageId          = lifecycle.lineageId
  final def target: ExecutionTarget            = lifecycle.target

final class SubmitOrderCommand[D <: Dim, B <: Dim, Q <: Dim] private (
  val commandId: ApplicationCommandId,
  val lifecycle: ExecutionLifecycle[D, B, Q])
  extends ExecutionCommand[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: SubmitOrderCommand[?, ?, ?] =>
      commandId == that.commandId && lifecycle == that.lifecycle
    case _ => false

  override def hashCode(): Int  = (commandId, lifecycle).hashCode
  override def toString: String = s"SubmitOrderCommand($commandId,$executionOrderId,$target)"

object SubmitOrderCommand:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    commandId: ApplicationCommandId,
    lifecycle: ExecutionLifecycle[D, B, Q]
  ): SubmitOrderCommand[D, B, Q] =
    new SubmitOrderCommand(commandId, lifecycle)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    commandId: ApplicationCommandId
  ): Either[CommandViolations, SubmitOrderCommand[D, B, Q]] =
    val violations = Vector(
      Option.when(lifecycle == null)(MissingCommandValue(CommandViolationLocation.Lifecycle)),
      Option.when(commandId == null)(MissingCommandValue(CommandViolationLocation.CommandIdentity))
    ).flatten
    CommandViolations.from(violations).toLeft(construct(commandId, lifecycle))
end SubmitOrderCommand
final class CancelOrderCommand[D <: Dim, B <: Dim, Q <: Dim] private (
  val commandId: ApplicationCommandId,
  val lifecycle: ExecutionLifecycle[D, B, Q],
  val originalSubmitCommandId: ApplicationCommandId)
  extends ExecutionCommand[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: CancelOrderCommand[?, ?, ?] =>
      commandId == that.commandId && lifecycle == that.lifecycle &&
      originalSubmitCommandId == that.originalSubmitCommandId
    case _ => false

  override def hashCode(): Int  = (commandId, lifecycle, originalSubmitCommandId).hashCode
  override def toString: String =
    s"CancelOrderCommand($commandId,$executionOrderId,$target,$originalSubmitCommandId)"

object CancelOrderCommand:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    commandId: ApplicationCommandId,
    lifecycle: ExecutionLifecycle[D, B, Q],
    originalSubmitCommandId: ApplicationCommandId
  ): CancelOrderCommand[D, B, Q] =
    new CancelOrderCommand(commandId, lifecycle, originalSubmitCommandId)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    commandId: ApplicationCommandId,
    originalSubmitCommandId: ApplicationCommandId
  ): Either[CommandViolations, CancelOrderCommand[D, B, Q]] =
    val violations = Vector(
      Option.when(lifecycle == null)(MissingCommandValue(CommandViolationLocation.Lifecycle)),
      Option.when(commandId == null)(MissingCommandValue(CommandViolationLocation.CommandIdentity)),
      Option.when(originalSubmitCommandId == null)(MissingCommandValue(CommandViolationLocation.OriginalSubmit))
    ).flatten
    CommandViolations.from(violations).toLeft(construct(commandId, lifecycle, originalSubmitCommandId))
end CancelOrderCommand

sealed abstract class DispatchEvidence[D <: Dim, B <: Dim, Q <: Dim] protected () extends JavaSerializationUnsupported:
  def submit: SubmitOrderCommand[D, B, Q]
  final def submitCommandId: ApplicationCommandId = submit.commandId

final class ProvenNotDispatched[D <: Dim, B <: Dim, Q <: Dim] private (
  val submit: SubmitOrderCommand[D, B, Q])
  extends DispatchEvidence[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: ProvenNotDispatched[?, ?, ?] => submit == that.submit
    case _                                  => false

  override def hashCode(): Int = ("proven-not-dispatched", submit).hashCode

object ProvenNotDispatched:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    submit: SubmitOrderCommand[D, B, Q]
  ): ProvenNotDispatched[D, B, Q] =
    new ProvenNotDispatched(submit)

  def forSubmit[D <: Dim, B <: Dim, Q <: Dim](
    submit: SubmitOrderCommand[D, B, Q]
  ): Either[CommandViolations, ProvenNotDispatched[D, B, Q]] =
    if submit == null then
      Left(CommandViolations.one(MissingCommandValue(CommandViolationLocation.OriginalSubmit)))
    else Right(construct(submit))
final class IndeterminateDispatch[D <: Dim, B <: Dim, Q <: Dim] private (
  val submit: SubmitOrderCommand[D, B, Q])
  extends DispatchEvidence[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: IndeterminateDispatch[?, ?, ?] => submit == that.submit
    case _                                    => false

  override def hashCode(): Int = ("indeterminate-dispatch", submit).hashCode

object IndeterminateDispatch:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    submit: SubmitOrderCommand[D, B, Q]
  ): IndeterminateDispatch[D, B, Q] =
    new IndeterminateDispatch(submit)

  def forSubmit[D <: Dim, B <: Dim, Q <: Dim](
    submit: SubmitOrderCommand[D, B, Q]
  ): Either[CommandViolations, IndeterminateDispatch[D, B, Q]] =
    if submit == null then
      Left(CommandViolations.one(MissingCommandValue(CommandViolationLocation.OriginalSubmit)))
    else Right(construct(submit))
final class CommandConflict[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  val original: ExecutionCommand[D, B, Q],
  val conflicting: ExecutionCommand[D, B, Q])
  extends JavaSerializationUnsupported:

  val commandId: ApplicationCommandId = original.commandId

  override def equals(other: Any): Boolean = other match
    case that: CommandConflict[?, ?, ?] =>
      original == that.original && conflicting == that.conflicting
    case _ => false

  override def hashCode(): Int = (original, conflicting).hashCode

sealed abstract class CommandTransition[D <: Dim, B <: Dim, Q <: Dim] protected () extends JavaSerializationUnsupported:
  def state: CommandState[D, B, Q]

final class AppliedCommandTransition[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  val state: CommandState[D, B, Q])
  extends CommandTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: AppliedCommandTransition[?, ?, ?] => state == that.state
    case _                                       => false

  override def hashCode(): Int = ("applied", state).hashCode

final class IdempotentCommandTransition[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  val state: CommandState[D, B, Q])
  extends CommandTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: IdempotentCommandTransition[?, ?, ?] => state == that.state
    case _                                          => false

  override def hashCode(): Int = ("idempotent", state).hashCode

final class ConflictingCommandTransition[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  val state: CommandState[D, B, Q])
  extends CommandTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: ConflictingCommandTransition[?, ?, ?] => state == that.state
    case _                                           => false

  override def hashCode(): Int = ("command-conflict", state).hashCode

final class ConflictingDispatchTransition[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  val state: CommandState[D, B, Q])
  extends CommandTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: ConflictingDispatchTransition[?, ?, ?] => state == that.state
    case _                                            => false

  override def hashCode(): Int = ("dispatch-conflict", state).hashCode

final class RejectedCommandTransition[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  val state: CommandState[D, B, Q],
  val violations: CommandViolations)
  extends CommandTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: RejectedCommandTransition[?, ?, ?] =>
      state == that.state && violations == that.violations
    case _ => false

  override def hashCode(): Int = (state, violations).hashCode
final class CommandState[D <: Dim, B <: Dim, Q <: Dim] private (
  val lifecycle: ExecutionLifecycle[D, B, Q],
  val issuedCommands: Map[ApplicationCommandId, ExecutionCommand[D, B, Q]],
  val dispatchKnowledge: Map[ApplicationCommandId, Vector[DispatchEvidence[D, B, Q]]],
  val conflicts: Vector[CommandConflict[D, B, Q]],
  val cancellationRequests: Vector[CancelOrderCommand[D, B, Q]])
  extends JavaSerializationUnsupported:

  def record(command: ExecutionCommand[D, B, Q]): CommandTransition[D, B, Q] =
    CommandState.record(this, command)

  def observeDispatch(evidence: DispatchEvidence[D, B, Q]): CommandTransition[D, B, Q] =
    CommandState.observeDispatch(this, evidence)

  override def equals(other: Any): Boolean = other match
    case that: CommandState[?, ?, ?] =>
      lifecycle == that.lifecycle && issuedCommands == that.issuedCommands &&
      dispatchKnowledge == that.dispatchKnowledge && conflicts == that.conflicts &&
      cancellationRequests == that.cancellationRequests
    case _ => false

  override def hashCode(): Int =
    (lifecycle, issuedCommands, dispatchKnowledge, conflicts, cancellationRequests).hashCode
end CommandState

object CommandState:
  private def constructState[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    issuedCommands: Map[ApplicationCommandId, ExecutionCommand[D, B, Q]],
    dispatchKnowledge: Map[ApplicationCommandId, Vector[DispatchEvidence[D, B, Q]]],
    conflicts: Vector[CommandConflict[D, B, Q]],
    cancellationRequests: Vector[CancelOrderCommand[D, B, Q]]
  ): CommandState[D, B, Q] =
    new CommandState(lifecycle, issuedCommands, dispatchKnowledge, conflicts, cancellationRequests)

  private def constructConflict[D <: Dim, B <: Dim, Q <: Dim](
    original: ExecutionCommand[D, B, Q],
    conflicting: ExecutionCommand[D, B, Q]
  ): CommandConflict[D, B, Q] =
    new CommandConflict(original, conflicting)

  private def applied[D <: Dim, B <: Dim, Q <: Dim](
    state: CommandState[D, B, Q]
  ): AppliedCommandTransition[D, B, Q] =
    new AppliedCommandTransition(state)

  private def idempotent[D <: Dim, B <: Dim, Q <: Dim](
    state: CommandState[D, B, Q]
  ): IdempotentCommandTransition[D, B, Q] =
    new IdempotentCommandTransition(state)

  private def commandConflict[D <: Dim, B <: Dim, Q <: Dim](
    state: CommandState[D, B, Q]
  ): ConflictingCommandTransition[D, B, Q] =
    new ConflictingCommandTransition(state)

  private def dispatchConflict[D <: Dim, B <: Dim, Q <: Dim](
    state: CommandState[D, B, Q]
  ): ConflictingDispatchTransition[D, B, Q] =
    new ConflictingDispatchTransition(state)

  private def rejected[D <: Dim, B <: Dim, Q <: Dim](
    state: CommandState[D, B, Q],
    violations: CommandViolations
  ): RejectedCommandTransition[D, B, Q] =
    new RejectedCommandTransition(state, violations)

  def initial[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  ): Either[CommandViolations, CommandState[D, B, Q]] =
    if lifecycle == null then
      Left(CommandViolations.one(MissingCommandValue(CommandViolationLocation.Lifecycle)))
    else Right(constructState(lifecycle, Map.empty, Map.empty, Vector.empty, Vector.empty))

  private def scopeViolations[D <: Dim, B <: Dim, Q <: Dim](
    expected: ExecutionLifecycle[D, B, Q],
    supplied: ExecutionLifecycle[D, B, Q],
    commandId: ApplicationCommandId
  ): Vector[CommandViolation] =
    if supplied == null then Vector(MissingCommandValue(CommandViolationLocation.Lifecycle))
    else
      val sameImmutableOrder =
        LifecycleValueSemantics.sameInstrument(expected.instrument, supplied.instrument) &&
          LifecycleValueSemantics.sameOrder(expected.order, supplied.order) &&
          expected.positionGrid.identity == supplied.positionGrid.identity &&
          expected.positionGrid.quantum == supplied.positionGrid.quantum
      Vector(
        Option.when(expected.executionOrderId != supplied.executionOrderId)(
          CommandLogicalOrderMismatch(expected.executionOrderId, supplied.executionOrderId)
        ),
        Option.when(expected.lineageId != supplied.lineageId)(
          CommandLineageMismatch(expected.lineageId, supplied.lineageId)
        ),
        Option.when(expected.target != supplied.target)(CommandTargetMismatch(expected.target, supplied.target)),
        Option.when(!sameImmutableOrder)(CommandImmutableOrderMismatch(commandId))
      ).flatten

  private def record[D <: Dim, B <: Dim, Q <: Dim](
    state: CommandState[D, B, Q],
    command: ExecutionCommand[D, B, Q]
  ): CommandTransition[D, B, Q] =
    if command == null then
      rejected(
        state,
        CommandViolations.one(MissingCommandValue(CommandViolationLocation.Command))
      )
    else
      state.issuedCommands.get(command.commandId) match
        case Some(existing) if existing == command =>
          idempotent(state)
        case Some(existing) =>
          val conflict = constructConflict(existing, command)
          val retained =
            if state.conflicts.contains(conflict) then state.conflicts else state.conflicts :+ conflict
          commandConflict(
            constructState(
              state.lifecycle,
              state.issuedCommands,
              state.dispatchKnowledge,
              retained,
              state.cancellationRequests
            )
          )
        case None =>
          val scope     = scopeViolations(state.lifecycle, command.lifecycle, command.commandId)
          val reference = command match
            case cancel: CancelOrderCommand[D, B, Q] =>
              state.issuedCommands.get(cancel.originalSubmitCommandId) match
                case None => Vector(UnknownOriginalSubmit(cancel.originalSubmitCommandId))
                case Some(_: SubmitOrderCommand[?, ?, ?]) => Vector.empty
                case Some(_) => Vector(ReferencedCommandIsNotSubmit(cancel.originalSubmitCommandId))
            case _: SubmitOrderCommand[D, B, Q] => Vector.empty
          CommandViolations.from(scope ++ reference) match
            case Some(violations) => rejected(state, violations)
            case None             =>
              val cancellations = command match
                case cancel: CancelOrderCommand[D, B, Q] => state.cancellationRequests :+ cancel
                case _: SubmitOrderCommand[D, B, Q]      => state.cancellationRequests
              val next = constructState(
                state.lifecycle,
                state.issuedCommands.updated(command.commandId, command),
                state.dispatchKnowledge,
                state.conflicts,
                cancellations
              )
              applied(next)

  private def observeDispatch[D <: Dim, B <: Dim, Q <: Dim](
    state: CommandState[D, B, Q],
    evidence: DispatchEvidence[D, B, Q]
  ): CommandTransition[D, B, Q] =
    if evidence == null then
      rejected(
        state,
        CommandViolations.one(MissingCommandValue(CommandViolationLocation.DispatchEvidence))
      )
    else
      val scope     = scopeViolations(state.lifecycle, evidence.submit.lifecycle, evidence.submitCommandId)
      val reference = state.issuedCommands.get(evidence.submitCommandId) match
        case None => Vector(UnknownOriginalSubmit(evidence.submitCommandId))
        case Some(submit: SubmitOrderCommand[?, ?, ?]) if submit != evidence.submit =>
          Vector(DispatchSubmitBodyMismatch(evidence.submitCommandId))
        case Some(_: SubmitOrderCommand[?, ?, ?]) => Vector.empty
        case Some(_)                              => Vector(ReferencedCommandIsNotSubmit(evidence.submitCommandId))
      CommandViolations.from(scope ++ reference) match
        case Some(violations) => rejected(state, violations)
        case None             =>
          val current = state.dispatchKnowledge.getOrElse(evidence.submitCommandId, Vector.empty)
          if current.contains(evidence) then idempotent(state)
          else
            val conflicting = current.nonEmpty
            val next        = constructState(
              state.lifecycle,
              state.issuedCommands,
              state.dispatchKnowledge.updated(evidence.submitCommandId, current :+ evidence),
              state.conflicts,
              state.cancellationRequests
            )
            if conflicting then dispatchConflict(next) else applied(next)
end CommandState
