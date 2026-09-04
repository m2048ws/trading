package trading.execution

import trading.economics.instrument.Lots
import trading.economics.instrument.PositionLots
import trading.economics.instrument.Price
import trading.order.Side
import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

enum ModifierAmbiguityKind extends JavaSerializationUnsupported:
  case ExplicitlyUnsequencedModifier
  case MultipleAuthoritativeStreams
  case ConflictingAuthoritativePosition
  case ModifierAfterBust

final class ModifierAmbiguity private (private val values: Vector[ModifierAmbiguityKind])
  extends JavaSerializationUnsupported:

  def head: ModifierAmbiguityKind             = values.head
  def toVector: Vector[ModifierAmbiguityKind] = values
  def size: Int                               = values.size

  override def equals(other: Any): Boolean = other match
    case that: ModifierAmbiguity => values == that.toVector
    case _                       => false
  override def hashCode(): Int = values.hashCode

object ModifierAmbiguity:
  private def construct(values: Vector[ModifierAmbiguityKind]): ModifierAmbiguity =
    new ModifierAmbiguity(values)

  private[execution] def from(values: Vector[ModifierAmbiguityKind]): Option[ModifierAmbiguity] =
    Option.when(values.nonEmpty)(construct(values.distinct))

sealed trait EffectiveFill[D <: Dim, B <: Dim, Q <: Dim] extends JavaSerializationUnsupported:
  def original: ExecutionFill[D, B, Q]
  def modifiers: Vector[FillModifier[D, B, Q]]

final case class ActiveEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  original: ExecutionFill[D, B, Q],
  effectiveLots: Lots[D],
  effectivePrice: Price[B, Q],
  modifiers: Vector[FillModifier[D, B, Q]])
  extends EffectiveFill[D, B, Q]

final case class BustedEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  original: ExecutionFill[D, B, Q],
  bust: FillBusted[D, B, Q],
  modifiers: Vector[FillModifier[D, B, Q]])
  extends EffectiveFill[D, B, Q]

final case class AmbiguousEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  original: ExecutionFill[D, B, Q],
  modifiers: Vector[FillModifier[D, B, Q]],
  ambiguity: ModifierAmbiguity)
  extends EffectiveFill[D, B, Q]

final case class ConflictingEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  original: ExecutionFill[D, B, Q],
  modifiers: Vector[FillModifier[D, B, Q]],
  eventConflicts: Vector[SourceFactConflict[D, B, Q]],
  identityConflicts: Vector[FillIdentityConflict[D, B, Q]])
  extends EffectiveFill[D, B, Q]

final case class OverfillAnomaly[D <: Dim](
  orderedLots: Lots[D],
  effectiveExposure: PositionLots[D],
  excessExposure: PositionLots[D])
  extends JavaSerializationUnsupported

final case class EffectiveFillLedger[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  byFillId: Map[QualifiedFillId, EffectiveFill[D, B, Q]],
  knownExposure: PositionLots[D],
  overfill: Option[OverfillAnomaly[D]],
  unresolvedReferences: Map[QualifiedFillId, Vector[UnresolvedFillReference[D, B, Q]]])
  extends JavaSerializationUnsupported

object EffectiveFillLedger:
  private[execution] def derive[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q]
  ): EffectiveFillLedger[D, B, Q] =
    val allFacts =
      (state.source.factsByEvent.values.toVector ++
        state.source.eventConflicts.flatMap(conflict => Vector(conflict.original, conflict.conflicting))).distinct
    val modifiers = allFacts.collect:
      case value: FillModifier[D, B, Q] => value
    val modifiersByFill      = modifiers.groupBy(_.referencedFillId)
    val eventConflictsByFill = state.source.eventConflicts
      .flatMap: conflict =>
        (referencedFillIds(conflict.original) ++ referencedFillIds(conflict.conflicting)).distinct.map(_ -> conflict)
      .groupMap(_._1)(_._2)
    val identityConflictsByFill = state.source.fillConflicts.groupBy(_.fillId)

    val effective = state.source.fillsById.map: (fillId, original) =>
      val targetModifiers = modifiersByFill
        .getOrElse(fillId, Vector.empty)
        .sorted(using ExecutionOrderings.fillModifier)
      val eventConflicts = eventConflictsByFill
        .getOrElse(fillId, Vector.empty)
        .distinct
        .sorted(using ExecutionOrderings.sourceFactConflict)
      val identityConflicts = identityConflictsByFill
        .getOrElse(fillId, Vector.empty)
        .sorted(using ExecutionOrderings.fillIdentityConflict)
      val value =
        if eventConflicts.nonEmpty || identityConflicts.nonEmpty then
          ConflictingEffectiveFill(original, targetModifiers, eventConflicts, identityConflicts)
        else resolve(state, original, targetModifiers)
      fillId -> value

    val activeLots = effective.values.collect:
      case value: ActiveEffectiveFill[D, B, Q] => value.effectiveLots.count.unrefined
    .sum
    val exposure = position(state.lifecycle, activeLots)
    val excess   = activeLots - state.lifecycle.orderedLots.count.unrefined
    val overfill = Option.when(excess > 0):
      OverfillAnomaly(state.lifecycle.orderedLots, exposure, position(state.lifecycle, excess))

    EffectiveFillLedger(effective, exposure, overfill, state.source.unresolvedFillReferences)
  end derive

  private def resolve[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    original: ExecutionFill[D, B, Q],
    modifiers: Vector[FillModifier[D, B, Q]]
  ): EffectiveFill[D, B, Q] =
    if modifiers.isEmpty then ActiveEffectiveFill(original, original.lots, original.price, Vector.empty)
    else
      val ordered             = modifiers.flatMap(modifier => modifier.authoritativePosition.map(_ -> modifier))
      val streams             = ordered.map(_._1.stream).distinct
      val conflictingPosition = ordered.exists: (position, _) =>
        state.source.positionConflicts.contains(position)
      val sorted = ordered.sortWith: (left, right) =>
        val positionComparison = ExecutionOrderings.comparePosition(left._1, right._1)
        if positionComparison != 0 then positionComparison < 0
        else ExecutionOrderings.compareFillModifier(left._2, right._2) < 0
      .map(_._2)
      val firstBust = sorted.indexWhere(_.isInstanceOf[FillBusted[?, ?, ?]])
      val ambiguity = ModifierAmbiguity.from(
        Vector(
          Option.when(ordered.size != modifiers.size)(ModifierAmbiguityKind.ExplicitlyUnsequencedModifier),
          Option.when(streams.size > 1)(ModifierAmbiguityKind.MultipleAuthoritativeStreams),
          Option.when(conflictingPosition)(ModifierAmbiguityKind.ConflictingAuthoritativePosition),
          Option.when(firstBust >= 0 && firstBust != sorted.size - 1)(ModifierAmbiguityKind.ModifierAfterBust)
        ).flatten
      )
      ambiguity match
        case Some(value) => AmbiguousEffectiveFill(original, modifiers, value)
        case None        =>
          sorted.last match
            case value: FillBusted[D, B, Q]    => BustedEffectiveFill(original, value, sorted)
            case value: FillCorrected[D, B, Q] =>
              ActiveEffectiveFill(original, value.replacementLots, value.replacementPrice, sorted)
  end resolve

  private def referencedFillIds(value: SourceFact[?, ?, ?]): Vector[QualifiedFillId] = value match
    case fill: ExecutionFill[?, ?, ?]    => Vector(fill.fillId)
    case modifier: FillModifier[?, ?, ?] => Vector(modifier.referencedFillId)
    case _                               => Vector.empty

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
end EffectiveFillLedger
