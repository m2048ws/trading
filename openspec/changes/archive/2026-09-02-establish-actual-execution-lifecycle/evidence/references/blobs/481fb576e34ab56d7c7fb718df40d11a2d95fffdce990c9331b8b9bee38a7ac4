package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

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

@nowarn("msg=Ignoring.*qualifier")
final class SubmissionConflicts private[this] (private val values: Vector[SubmissionConflictKind])
  extends JavaSerializationUnsupported:

  def head: SubmissionConflictKind             = values.head
  def toVector: Vector[SubmissionConflictKind] = values
  def size: Int                                = values.size

  override def equals(other: Any): Boolean = other match
    case that: SubmissionConflicts => values == that.toVector
    case _                         => false
  override def hashCode(): Int = values.hashCode

object SubmissionConflicts:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SubmissionConflicts], MethodHandles.lookup())
      .findConstructor(classOf[SubmissionConflicts], MethodType.methodType(classOf[Unit], classOf[Vector[?]]))

  private def construct(values: Vector[SubmissionConflictKind]): SubmissionConflicts =
    constructor.invoke(values).asInstanceOf[SubmissionConflicts]

  private[execution] def from(values: Vector[SubmissionConflictKind]): Option[SubmissionConflicts] =
    Option.when(values.nonEmpty)(construct(values.distinct))

@nowarn("msg=Ignoring.*qualifier")
final class SubmissionEvidence[D <: Dim, B <: Dim, Q <: Dim] private[this] (
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

sealed abstract class SubmissionKnowledge[D <: Dim, B <: Dim, Q <: Dim] protected ()
  extends JavaSerializationUnsupported:
  SubmissionKnowledge.requireBuiltin(this)
  def evidence: SubmissionEvidence[D, B, Q]

@nowarn("msg=Ignoring.*qualifier")
final class IssuedPendingSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: IssuedPendingSubmission[?, ?, ?] => evidence == that.evidence
    case _                                      => false
  override def hashCode(): Int = ("issued-pending", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class AcceptedSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: AcceptedSubmission[?, ?, ?] => evidence == that.evidence
    case _                                 => false
  override def hashCode(): Int = ("accepted", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class RejectedSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: RejectedSubmission[?, ?, ?] => evidence == that.evidence
    case _                                 => false
  override def hashCode(): Int = ("rejected", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class ProvenNotDispatchedSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: ProvenNotDispatchedSubmission[?, ?, ?] => evidence == that.evidence
    case _                                            => false
  override def hashCode(): Int = ("proven-not-dispatched", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class IndeterminateSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: IndeterminateSubmission[?, ?, ?] => evidence == that.evidence
    case _                                      => false
  override def hashCode(): Int = ("indeterminate", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class ExecutionProvenSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: ExecutionProvenSubmission[?, ?, ?] => evidence == that.evidence
    case _                                        => false
  override def hashCode(): Int = ("execution-proven", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class AuthoritativelyAbsentSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q])
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: AuthoritativelyAbsentSubmission[?, ?, ?] => evidence == that.evidence
    case _                                              => false
  override def hashCode(): Int = ("authoritatively-absent", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class ConflictingSubmission[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: SubmissionEvidence[D, B, Q],
  val conflicts: SubmissionConflicts)
  extends SubmissionKnowledge[D, B, Q]():
  override def equals(other: Any): Boolean = other match
    case that: ConflictingSubmission[?, ?, ?] => evidence == that.evidence && conflicts == that.conflicts
    case _                                    => false
  override def hashCode(): Int = ("conflicting", evidence, conflicts).hashCode

object SubmissionKnowledge:
  private val evidenceConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SubmissionEvidence[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[SubmissionEvidence[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]]
        )
      )

  private def oneArgumentConstructor(runtimeClass: Class[?]): MethodHandle =
    MethodHandles
      .privateLookupIn(runtimeClass, MethodHandles.lookup())
      .findConstructor(
        runtimeClass,
        MethodType.methodType(classOf[Unit], classOf[SubmissionEvidence[?, ?, ?]])
      )

  private val issuedConstructor        = oneArgumentConstructor(classOf[IssuedPendingSubmission[?, ?, ?]])
  private val acceptedConstructor      = oneArgumentConstructor(classOf[AcceptedSubmission[?, ?, ?]])
  private val rejectedConstructor      = oneArgumentConstructor(classOf[RejectedSubmission[?, ?, ?]])
  private val notDispatchedConstructor = oneArgumentConstructor(classOf[ProvenNotDispatchedSubmission[?, ?, ?]])
  private val indeterminateConstructor = oneArgumentConstructor(classOf[IndeterminateSubmission[?, ?, ?]])
  private val executionConstructor     = oneArgumentConstructor(classOf[ExecutionProvenSubmission[?, ?, ?]])
  private val absentConstructor        = oneArgumentConstructor(classOf[AuthoritativelyAbsentSubmission[?, ?, ?]])
  private val conflictingConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ConflictingSubmission[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[ConflictingSubmission[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[SubmissionEvidence[?, ?, ?]],
          classOf[SubmissionConflicts]
        )
      )

  private def constructEvidence[D <: Dim, B <: Dim, Q <: Dim](
    submits: Set[SubmitOrderCommand[D, B, Q]],
    dispatch: Set[DispatchEvidence[D, B, Q]],
    acceptances: Set[OrderAccepted[D, B, Q]],
    rejections: Set[OrderRejected[D, B, Q]],
    fills: Set[ExecutionFill[D, B, Q]],
    absences: Set[SourceOrderAbsent[D, B, Q]]
  ): SubmissionEvidence[D, B, Q] =
    evidenceConstructor
      .invoke(submits, dispatch, acceptances, rejections, fills, absences)
      .asInstanceOf[SubmissionEvidence[D, B, Q]]

  private def constructOne[D <: Dim, B <: Dim, Q <: Dim, A <: SubmissionKnowledge[D, B, Q]](
    constructor: MethodHandle,
    evidence: SubmissionEvidence[D, B, Q]
  ): A = constructor.invoke(evidence).asInstanceOf[A]

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
        Some(
          conflictingConstructor
            .invoke(evidence, values)
            .asInstanceOf[ConflictingSubmission[D, B, Q]]
        )
      case None if acceptances.nonEmpty =>
        Some(constructOne(acceptedConstructor, evidence))
      case None if rejections.nonEmpty =>
        Some(constructOne(rejectedConstructor, evidence))
      case None if fills.nonEmpty =>
        Some(constructOne(executionConstructor, evidence))
      case None if proven =>
        Some(constructOne(notDispatchedConstructor, evidence))
      case None if authoritativeAbsence =>
        Some(constructOne(absentConstructor, evidence))
      case None if indeterminate =>
        Some(constructOne(indeterminateConstructor, evidence))
      case None if submits.nonEmpty =>
        Some(constructOne(issuedConstructor, evidence))
      case None => None
    end match
  end derive

  private[execution] def requireBuiltin(value: SubmissionKnowledge[?, ?, ?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass == classOf[IssuedPendingSubmission[?, ?, ?]] ||
        runtimeClass == classOf[AcceptedSubmission[?, ?, ?]] ||
        runtimeClass == classOf[RejectedSubmission[?, ?, ?]] ||
        runtimeClass == classOf[ProvenNotDispatchedSubmission[?, ?, ?]] ||
        runtimeClass == classOf[IndeterminateSubmission[?, ?, ?]] ||
        runtimeClass == classOf[ExecutionProvenSubmission[?, ?, ?]] ||
        runtimeClass == classOf[AuthoritativelyAbsentSubmission[?, ?, ?]] ||
        runtimeClass == classOf[ConflictingSubmission[?, ?, ?]]
    if !supported then
      throw new IllegalAccessError(s"unsupported SubmissionKnowledge implementation: ${runtimeClass.getName}")
end SubmissionKnowledge
