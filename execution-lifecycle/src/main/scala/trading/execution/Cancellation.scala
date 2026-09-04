package trading.execution

import trading.economics.instrument.PositionLots
import trading.order.Side
import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported
final case class CancellationEvidence[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  issuedRequests: Set[CancelOrderCommand[D, B, Q]],
  reportedRequests: Set[CancelOrderCommand[D, B, Q]],
  referencedSubmissions: Set[SubmitOrderCommand[D, B, Q]],
  reportedConfirmations: Set[CancellationEffective[D, B, Q]],
  authoritativeConfirmations: Set[CancellationEffective[D, B, Q]],
  commandConflicts: Set[CommandConflict[D, B, Q]],
  sourceConflicts: Set[SourceFactConflict[D, B, Q]])
  extends JavaSerializationUnsupported

sealed trait CancellationKnowledge[D <: Dim, B <: Dim, Q <: Dim] extends JavaSerializationUnsupported:
  def evidence: CancellationEvidence[D, B, Q]

final case class CancellationRequested[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: CancellationEvidence[D, B, Q])
  extends CancellationKnowledge[D, B, Q]

final case class CancellationConfirmed[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: CancellationEvidence[D, B, Q])
  extends CancellationKnowledge[D, B, Q]

final case class CancellationConflicted[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  evidence: CancellationEvidence[D, B, Q])
  extends CancellationKnowledge[D, B, Q]

object CancellationKnowledge:
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
    val evidence = CancellationEvidence(
      issuedRequests,
      reportedRequests,
      referencedSubmissions,
      reportedConfirmations,
      authoritative,
      relevantCommandConflicts,
      relevantSourceConflicts
    )

    if authoritative.nonEmpty then Some(CancellationConfirmed(evidence))
    else if issuedRequests.nonEmpty then Some(CancellationRequested(evidence))
    else if reportedRequests.nonEmpty || reportedConfirmations.nonEmpty then Some(CancellationConflicted(evidence))
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
final case class PostCancellationFillAnomaly[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  effectiveFill: ActiveEffectiveFill[D, B, Q],
  priorCancellations: Vector[CancellationEffective[D, B, Q]],
  exactExposure: PositionLots[D])
  extends JavaSerializationUnsupported:

  val fillId: QualifiedFillId = effectiveFill.original.fillId

final case class ExecutionAnomalies[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  overfill: Option[OverfillAnomaly[D]],
  postCancellationFills: Vector[PostCancellationFillAnomaly[D, B, Q]],
  sourceEventConflicts: Set[SourceFactConflict[D, B, Q]],
  fillIdentityConflicts: Set[FillIdentityConflict[D, B, Q]],
  streamPositionConflicts: Map[QualifiedStreamPosition, StreamPositionConflict[D, B, Q]])
  extends JavaSerializationUnsupported:

  def isEmpty: Boolean =
    overfill.isEmpty && postCancellationFills.isEmpty && sourceEventConflicts.isEmpty &&
      fillIdentityConflicts.isEmpty && streamPositionConflicts.isEmpty

end ExecutionAnomalies

object ExecutionAnomalies:
  private[execution] def derive[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    ledger: EffectiveFillLedger[D, B, Q]
  ): ExecutionAnomalies[D, B, Q] =
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
              .sortWith: (left, right) =>
                val positionComparison = ExecutionOrderings.comparePosition(left._1, right._1)
                if positionComparison != 0 then positionComparison < 0
                else ExecutionOrderings.compareCancellation(left._2, right._2) < 0
              .map(_._2)
            Option.when(prior.nonEmpty):
              PostCancellationFillAnomaly(
                active,
                prior,
                position(state.lifecycle, active.effectiveLots.count.unrefined)
              )
            .toVector
      case _ => Vector.empty
    .sorted(using ExecutionOrderings.postCancellationFillAnomaly)

    ExecutionAnomalies(
      ledger.overfill,
      postCancellationFills,
      state.source.eventConflicts.toSet,
      state.source.fillConflicts.toSet,
      state.source.positionConflicts
    )
  end derive

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
