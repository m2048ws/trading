package trading.execution

import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

enum SubmissionConflictKind extends JavaSerializationUnsupported:
  case DispatchOutcomeConflict
  case ProvenNonDispatchVersusSourceOutcome
  case AcceptanceVersusRejection
  case AcceptanceVersusAuthoritativeAbsence
  case RejectionVersusExecution
  case RejectionVersusAuthoritativeAbsence
  case ExecutionVersusAuthoritativeAbsence
final class SubmissionConflicts private (private val values: Vector[SubmissionConflictKind])
  extends JavaSerializationUnsupported:

  def head: SubmissionConflictKind             = values.head
  def toVector: Vector[SubmissionConflictKind] = values
  def size: Int                                = values.size

  override def equals(other: Any): Boolean = other match
    case that: SubmissionConflicts => values == that.toVector
    case _                         => false
  override def hashCode(): Int = values.hashCode

object SubmissionConflicts:
  private def construct(values: Vector[SubmissionConflictKind]): SubmissionConflicts =
    new SubmissionConflicts(values)

  private[execution] def from(values: Vector[SubmissionConflictKind]): Option[SubmissionConflicts] =
    Option.when(values.nonEmpty)(construct(values.distinct))
final class SubmissionEvidence[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  val submitCommands: Set[SubmitOrderCommand[D, B, Q]],
  val dispatchEvidence: Set[DispatchEvidence[D, B, Q]],
  val acceptances: Set[OrderAccepted[D, B, Q]],
  val rejections: Set[OrderRejected[D, B, Q]],
  val executionFills: Set[ExecutionFill[D, B, Q]],
  val sourceAbsences: Set[SourceOrderAbsent[D, B, Q]])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: SubmissionEvidence[?, ?, ?] =>
      submitCommands == that.submitCommands && dispatchEvidence == that.dispatchEvidence &&
      acceptances == that.acceptances && rejections == that.rejections &&
      executionFills == that.executionFills && sourceAbsences == that.sourceAbsences
    case _ => false
  override def hashCode(): Int =
    (submitCommands, dispatchEvidence, acceptances, rejections, executionFills, sourceAbsences).hashCode

sealed trait SubmissionKnowledge[D <: Dim, B <: Dim, Q <: Dim] extends JavaSerializationUnsupported:
  def evidence: SubmissionEvidence[D, B, Q]

final case class IssuedPendingSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]

final case class AcceptedSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]

final case class RejectedSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]

final case class ProvenNotDispatchedSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]

final case class IndeterminateSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]

final case class ExecutionProvenSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]

final case class AuthoritativelyAbsentSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]

final case class ConflictingSubmission[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: SubmissionEvidence[D, B, Q],
  conflicts: SubmissionConflicts)
  extends SubmissionKnowledge[D, B, Q]

object SubmissionKnowledge:
  private def constructEvidence[D <: Dim, B <: Dim, Q <: Dim](
    submits: Set[SubmitOrderCommand[D, B, Q]],
    dispatch: Set[DispatchEvidence[D, B, Q]],
    acceptances: Set[OrderAccepted[D, B, Q]],
    rejections: Set[OrderRejected[D, B, Q]],
    fills: Set[ExecutionFill[D, B, Q]],
    absences: Set[SourceOrderAbsent[D, B, Q]]
  ): SubmissionEvidence[D, B, Q] =
    new SubmissionEvidence(submits, dispatch, acceptances, rejections, fills, absences)

  private[execution] def derive[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    authoritativeStreams: Set[QualifiedSourceStreamId]
  ): Option[SubmissionKnowledge[D, B, Q]] =
    val commandEvidence =
      state.commands.issuedCommands.values.toSet ++
        state.commands.conflicts.flatMap(conflict => Vector(conflict.original, conflict.conflicting))
    val submits = commandEvidence.collect:
      case value: SubmitOrderCommand[D, B, Q] => value
    val dispatch       = state.commands.dispatchKnowledge.valuesIterator.flatten.toSet
    val sourceEvidence =
      state.source.factsByEvent.values.toSet ++
        state.source.eventConflicts.flatMap(conflict => Vector(conflict.original, conflict.conflicting))
    val acceptances = sourceEvidence.collect:
      case value: OrderAccepted[D, B, Q] => value
    val rejections = sourceEvidence.collect:
      case value: OrderRejected[D, B, Q] => value
    val fills = sourceEvidence.collect:
      case value: ExecutionFill[D, B, Q] => value
    val absences = sourceEvidence.collect:
      case value: SourceOrderAbsent[D, B, Q] => value
    val evidence = constructEvidence(submits, dispatch, acceptances, rejections, fills, absences)

    val proven               = dispatch.exists(_.isInstanceOf[ProvenNotDispatched[?, ?, ?]])
    val indeterminate        = dispatch.exists(_.isInstanceOf[IndeterminateDispatch[?, ?, ?]])
    val authoritativeAbsence = absences.exists: absence =>
      authoritativeStreams.contains(absence.completeness.completeThrough.stream)
    val sourceOutcome = acceptances.nonEmpty || rejections.nonEmpty || fills.nonEmpty
    val conflicts     = Vector(
      Option.when(proven && indeterminate)(SubmissionConflictKind.DispatchOutcomeConflict),
      Option.when(proven && sourceOutcome)(SubmissionConflictKind.ProvenNonDispatchVersusSourceOutcome),
      Option.when(acceptances.nonEmpty && rejections.nonEmpty)(SubmissionConflictKind.AcceptanceVersusRejection),
      Option.when(acceptances.nonEmpty && authoritativeAbsence)(
        SubmissionConflictKind.AcceptanceVersusAuthoritativeAbsence
      ),
      Option.when(rejections.nonEmpty && fills.nonEmpty)(SubmissionConflictKind.RejectionVersusExecution),
      Option.when(rejections.nonEmpty && authoritativeAbsence)(
        SubmissionConflictKind.RejectionVersusAuthoritativeAbsence
      ),
      Option.when(fills.nonEmpty && authoritativeAbsence)(
        SubmissionConflictKind.ExecutionVersusAuthoritativeAbsence
      )
    ).flatten

    SubmissionConflicts.from(conflicts) match
      case Some(values) =>
        Some(ConflictingSubmission(evidence, values))
      case None if acceptances.nonEmpty =>
        Some(AcceptedSubmission(evidence))
      case None if rejections.nonEmpty =>
        Some(RejectedSubmission(evidence))
      case None if fills.nonEmpty =>
        Some(ExecutionProvenSubmission(evidence))
      case None if proven =>
        Some(ProvenNotDispatchedSubmission(evidence))
      case None if authoritativeAbsence =>
        Some(AuthoritativelyAbsentSubmission(evidence))
      case None if indeterminate =>
        Some(IndeterminateSubmission(evidence))
      case None if submits.nonEmpty =>
        Some(IssuedPendingSubmission(evidence))
      case None => None
    end match
  end derive
end SubmissionKnowledge
