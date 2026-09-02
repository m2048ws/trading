package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.economics.instrument.PositionLots
import trading.order.Side
import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

@nowarn("msg=Ignoring.*qualifier")
final class CancellationEvidence[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val issuedRequests: Set[CancelOrderCommand[D, B, Q]],
  val reportedRequests: Set[CancelOrderCommand[D, B, Q]],
  val referencedSubmissions: Set[SubmitOrderCommand[D, B, Q]],
  val reportedConfirmations: Set[CancellationEffective[D, B, Q]],
  val authoritativeConfirmations: Set[CancellationEffective[D, B, Q]],
  val commandConflicts: Set[CommandConflict[D, B, Q]],
  val sourceConflicts: Set[SourceFactConflict[D, B, Q]])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: CancellationEvidence[?, ?, ?] =>
      issuedRequests == that.issuedRequests && reportedRequests == that.reportedRequests &&
      referencedSubmissions == that.referencedSubmissions &&
      reportedConfirmations == that.reportedConfirmations &&
      authoritativeConfirmations == that.authoritativeConfirmations &&
      commandConflicts == that.commandConflicts && sourceConflicts == that.sourceConflicts
    case _ => false

  override def hashCode(): Int =
    (
      issuedRequests,
      reportedRequests,
      referencedSubmissions,
      reportedConfirmations,
      authoritativeConfirmations,
      commandConflicts,
      sourceConflicts
    ).hashCode
end CancellationEvidence

sealed abstract class CancellationKnowledge[D <: Dim, B <: Dim, Q <: Dim] protected ()
  extends JavaSerializationUnsupported:
  CancellationKnowledge.requireBuiltin(this)
  def evidence: CancellationEvidence[D, B, Q]

@nowarn("msg=Ignoring.*qualifier")
final class CancellationRequested[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: CancellationEvidence[D, B, Q])
  extends CancellationKnowledge[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: CancellationRequested[?, ?, ?] => evidence == that.evidence
    case _                                    => false
  override def hashCode(): Int = ("cancellation-requested", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class CancellationConfirmed[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: CancellationEvidence[D, B, Q])
  extends CancellationKnowledge[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: CancellationConfirmed[?, ?, ?] => evidence == that.evidence
    case _                                    => false
  override def hashCode(): Int = ("cancellation-confirmed", evidence).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class CancellationConflicted[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val evidence: CancellationEvidence[D, B, Q])
  extends CancellationKnowledge[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: CancellationConflicted[?, ?, ?] => evidence == that.evidence
    case _                                     => false
  override def hashCode(): Int = ("cancellation-conflicted", evidence).hashCode

object CancellationKnowledge:
  private val evidenceConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[CancellationEvidence[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[CancellationEvidence[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Set[?]]
        )
      )

  private val requestedConstructor: MethodHandle =
    knowledgeConstructor(classOf[CancellationRequested[?, ?, ?]])

  private val confirmedConstructor: MethodHandle =
    knowledgeConstructor(classOf[CancellationConfirmed[?, ?, ?]])

  private val conflictedConstructor: MethodHandle =
    knowledgeConstructor(classOf[CancellationConflicted[?, ?, ?]])

  private def knowledgeConstructor(owner: Class[?]): MethodHandle =
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(owner, MethodType.methodType(classOf[Unit], classOf[CancellationEvidence[?, ?, ?]]))

  private def constructEvidence[D <: Dim, B <: Dim, Q <: Dim](
    issuedRequests: Set[CancelOrderCommand[D, B, Q]],
    reportedRequests: Set[CancelOrderCommand[D, B, Q]],
    referencedSubmissions: Set[SubmitOrderCommand[D, B, Q]],
    reportedConfirmations: Set[CancellationEffective[D, B, Q]],
    authoritativeConfirmations: Set[CancellationEffective[D, B, Q]],
    commandConflicts: Set[CommandConflict[D, B, Q]],
    sourceConflicts: Set[SourceFactConflict[D, B, Q]]
  ): CancellationEvidence[D, B, Q] =
    evidenceConstructor
      .invoke(
        issuedRequests,
        reportedRequests,
        referencedSubmissions,
        reportedConfirmations,
        authoritativeConfirmations,
        commandConflicts,
        sourceConflicts
      )
      .asInstanceOf[CancellationEvidence[D, B, Q]]

  private def requested[D <: Dim, B <: Dim, Q <: Dim](
    evidence: CancellationEvidence[D, B, Q]
  ): CancellationRequested[D, B, Q] =
    requestedConstructor.invoke(evidence).asInstanceOf[CancellationRequested[D, B, Q]]

  private def confirmed[D <: Dim, B <: Dim, Q <: Dim](
    evidence: CancellationEvidence[D, B, Q]
  ): CancellationConfirmed[D, B, Q] =
    confirmedConstructor.invoke(evidence).asInstanceOf[CancellationConfirmed[D, B, Q]]

  private def conflicted[D <: Dim, B <: Dim, Q <: Dim](
    evidence: CancellationEvidence[D, B, Q]
  ): CancellationConflicted[D, B, Q] =
    conflictedConstructor.invoke(evidence).asInstanceOf[CancellationConflicted[D, B, Q]]

  private[execution] def requireBuiltin(value: CancellationKnowledge[?, ?, ?]): Unit =
    val runtimeClass = value.getClass
    if runtimeClass != classOf[CancellationRequested[?, ?, ?]] &&
      runtimeClass != classOf[CancellationConfirmed[?, ?, ?]] &&
      runtimeClass != classOf[CancellationConflicted[?, ?, ?]]
    then throw new IllegalAccessError(s"unsupported CancellationKnowledge implementation: ${runtimeClass.getName}")

  private[execution] def derive[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q]
  ): Option[CancellationKnowledge[D, B, Q]] =
    val relevantCommandConflicts = state.commands.conflicts.filter: conflict =>
      isCancellation(conflict.original) || isCancellation(conflict.conflicting)
    .toSet
    val relevantSourceConflicts = state.source.eventConflicts.filter: conflict =>
      isCancellation(conflict.original) || isCancellation(conflict.conflicting)
    .toSet
    val issuedRequests   = state.commands.cancellationRequests.toSet
    val reportedRequests =
      issuedRequests ++ relevantCommandConflicts.flatMap: conflict =>
        Vector(conflict.original, conflict.conflicting).collect:
          case value: CancelOrderCommand[D, B, Q] => value
    val canonicalConfirmations = state.source.factsByEvent.values.collect:
      case value: CancellationEffective[D, B, Q] => value
    .toSet
    val reportedConfirmations =
      canonicalConfirmations ++ relevantSourceConflicts.flatMap: conflict =>
        Vector(conflict.original, conflict.conflicting).collect:
          case value: CancellationEffective[D, B, Q] => value
    val conflictedEvents      = relevantSourceConflicts.map(_.eventId)
    val authoritative         = canonicalConfirmations.filterNot(value => conflictedEvents.contains(value.eventId))
    val referencedSubmissions = reportedRequests.flatMap: request =>
      state.commands.issuedCommands.get(request.originalSubmitCommandId).collect:
        case value: SubmitOrderCommand[D, B, Q] => value
    val evidence = constructEvidence(
      issuedRequests,
      reportedRequests,
      referencedSubmissions,
      reportedConfirmations,
      authoritative,
      relevantCommandConflicts,
      relevantSourceConflicts
    )

    if authoritative.nonEmpty then Some(confirmed(evidence))
    else if issuedRequests.nonEmpty then Some(requested(evidence))
    else if reportedRequests.nonEmpty || reportedConfirmations.nonEmpty then Some(conflicted(evidence))
    else None
  end derive

  private[execution] def authoritativeConfirmations[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q]
  ): Set[CancellationEffective[D, B, Q]] =
    derive(state).collect:
      case value: CancellationConfirmed[D, B, Q] => value.evidence.authoritativeConfirmations
    .getOrElse(Set.empty)

  private def isCancellation(value: ExecutionCommand[?, ?, ?]): Boolean =
    value.isInstanceOf[CancelOrderCommand[?, ?, ?]]

  private def isCancellation(value: SourceFact[?, ?, ?]): Boolean =
    value.isInstanceOf[CancellationEffective[?, ?, ?]]
end CancellationKnowledge

@nowarn("msg=Ignoring.*qualifier")
final class PostCancellationFillAnomaly[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val effectiveFill: ActiveEffectiveFill[D, B, Q],
  val priorCancellations: Vector[CancellationEffective[D, B, Q]],
  val exactExposure: PositionLots[D])
  extends JavaSerializationUnsupported:

  val fillId: QualifiedFillId = effectiveFill.original.fillId

  override def equals(other: Any): Boolean = other match
    case that: PostCancellationFillAnomaly[?, ?, ?] =>
      effectiveFill == that.effectiveFill && priorCancellations == that.priorCancellations &&
      exactExposure == that.exactExposure
    case _ => false
  override def hashCode(): Int = (effectiveFill, priorCancellations, exactExposure).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class ExecutionAnomalies[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val overfill: Option[OverfillAnomaly[D]],
  val postCancellationFills: Vector[PostCancellationFillAnomaly[D, B, Q]],
  val sourceEventConflicts: Set[SourceFactConflict[D, B, Q]],
  val fillIdentityConflicts: Set[FillIdentityConflict[D, B, Q]],
  val streamPositionConflicts: Map[QualifiedStreamPosition, StreamPositionConflict[D, B, Q]])
  extends JavaSerializationUnsupported:

  def isEmpty: Boolean =
    overfill.isEmpty && postCancellationFills.isEmpty && sourceEventConflicts.isEmpty &&
      fillIdentityConflicts.isEmpty && streamPositionConflicts.isEmpty

  override def equals(other: Any): Boolean = other match
    case that: ExecutionAnomalies[?, ?, ?] =>
      overfill == that.overfill && postCancellationFills == that.postCancellationFills &&
      sourceEventConflicts == that.sourceEventConflicts && fillIdentityConflicts == that.fillIdentityConflicts &&
      streamPositionConflicts == that.streamPositionConflicts
    case _ => false
  override def hashCode(): Int =
    (overfill, postCancellationFills, sourceEventConflicts, fillIdentityConflicts, streamPositionConflicts).hashCode
end ExecutionAnomalies

object ExecutionAnomalies:
  private val anomalyConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[PostCancellationFillAnomaly[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[PostCancellationFillAnomaly[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ActiveEffectiveFill[?, ?, ?]],
          classOf[Vector[?]],
          classOf[PositionLots[?]]
        )
      )

  private val anomaliesConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionAnomalies[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[ExecutionAnomalies[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[Option[?]],
          classOf[Vector[?]],
          classOf[Set[?]],
          classOf[Set[?]],
          classOf[Map[?, ?]]
        )
      )

  private def postCancellation[D <: Dim, B <: Dim, Q <: Dim](
    effectiveFill: ActiveEffectiveFill[D, B, Q],
    priorCancellations: Vector[CancellationEffective[D, B, Q]],
    exactExposure: PositionLots[D]
  ): PostCancellationFillAnomaly[D, B, Q] =
    anomalyConstructor
      .invoke(effectiveFill, priorCancellations, exactExposure)
      .asInstanceOf[PostCancellationFillAnomaly[D, B, Q]]

  private[execution] def derive[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q]
  ): ExecutionAnomalies[D, B, Q] =
    val ledger               = EffectiveFillLedger.derive(state)
    val orderedCancellations = CancellationKnowledge
      .authoritativeConfirmations(state)
      .flatMap: cancellation =>
        cancellation.authoritativePosition.collect:
          case position if !state.source.positionConflicts.contains(position) => position -> cancellation
      .toVector

    val postCancellationFills = ledger.byFillId.values.toVector.flatMap:
      case active: ActiveEffectiveFill[D, B, Q] =>
        active.original.authoritativePosition.toVector.flatMap: fillPosition =>
          if state.source.positionConflicts.contains(fillPosition) then Vector.empty
          else
            val prior = orderedCancellations
              .collect:
                case (cancelPosition, cancellation)
                  if cancelPosition.stream == fillPosition.stream &&
                    cancelPosition.sequence.value < fillPosition.sequence.value =>
                  cancelPosition -> cancellation
              .sortBy: (position, cancellation) =>
                (position.sequence.value, eventKey(cancellation.eventId))
              .map(_._2)
            Option.when(prior.nonEmpty):
              postCancellation(
                active,
                prior,
                position(state.lifecycle, active.effectiveLots.count.unrefined)
              )
            .toVector
      case _ => Vector.empty
    .sortBy: anomaly =>
      val position = anomaly.effectiveFill.original.authoritativePosition.get
      (streamKey(position.stream), position.sequence.value, fillKey(anomaly.fillId))

    anomaliesConstructor
      .invoke(
        ledger.overfill,
        postCancellationFills,
        state.source.eventConflicts.toSet,
        state.source.fillConflicts.toSet,
        state.source.positionConflicts
      )
      .asInstanceOf[ExecutionAnomalies[D, B, Q]]
  end derive

  private def eventKey(value: QualifiedSourceEventId): String =
    s"${value.target.source.value}-${value.target.account.value}-${value.native.value}"

  private def fillKey(value: QualifiedFillId): String =
    s"${value.target.source.value}-${value.target.account.value}-${value.native.value}"

  private def streamKey(value: QualifiedSourceStreamId): String =
    s"${value.target.source.value}-${value.target.account.value}-${value.native.value}"

  private def position[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    count: BigInt
  ): PositionLots[D] =
    val coordinate = lifecycle.order.intent.side match
      case Side.Buy  => count
      case Side.Sell => -count
    PositionLots
      .fromCoordinate(lifecycle.instrument)(coordinate)
      .asInstanceOf[PositionLots[D]]
end ExecutionAnomalies
